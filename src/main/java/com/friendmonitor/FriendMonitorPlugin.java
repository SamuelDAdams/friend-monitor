package com.friendmonitor;

import com.friendmonitor.account.AccountSession;
import com.friendmonitor.account.runescape.RunescapeAccountSession;
import com.friendmonitor.account.runescape.RunescapeAccountSessionListener;
import com.friendmonitor.account.runescape.socket.server.FriendDeathMessage;
import com.friendmonitor.activityupdate.models.ActivityUpdate;
import com.friendmonitor.activityupdate.models.Location;
import com.friendmonitor.activityupdate.models.PlayerDeath;
import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import okhttp3.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Friend Monitor"
)
public class FriendMonitorPlugin extends Plugin implements RunescapeAccountSessionListener, AuthenticationClientListener
{
	private static final String GET_ACTIVITY = "Get Activity";
	private static final int FRIEND_CHAT_STATUS_VARBIT = 13674;
	@Getter
	@Setter
	private int tickcounter = 0;

	@Getter
	@Setter
	private boolean skillsLoaded = false;

	@Getter
	@Setter
	private boolean varbitChangedQuestsNeedToBeChecked = false;

	private boolean needsToCreateRunescapeAccountSession = true;

	@Getter
	@Setter
	private Map<Skill, Integer> skills;

	@Getter
	@Setter
	private List<Quest> uncompletedQuests;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private FriendMonitorConfig config;

	@Inject
	private AuthenticationClient authenticationClient;

	@Inject
	private Gson gson;

	@Inject
	private WorldMapPointManager worldMapManager;

	private AccountSession accountSession;
	private RunescapeAccountSession runescapeAccountSession;

	@Override
	protected void startUp() throws Exception
	{
		authenticationClient.setListener(this);
		authenticationClient.login();
	}

	@Override
	protected void shutDown() throws Exception
	{
		stop(true);
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN && accountSession != null && runescapeAccountSession == null) {
			needsToCreateRunescapeAccountSession = true;
		}
		else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
			stop(false);
		}
	}

	private void stop(boolean stopAccount) {
		needsToCreateRunescapeAccountSession = false;

		if (runescapeAccountSession != null) {
			runescapeAccountSession.stop();
		}

		runescapeAccountSession = null;
		skillsLoaded = false;
		skills.clear();
		uncompletedQuests.clear();
		uncompletedQuests = null;

		if (stopAccount) {
			accountSession = null;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged stat) {
		if (!skillsLoaded) {
			return;
		}

		System.out.println("Stat: " + stat.getSkill() + "lvl: " + stat.getLevel() + " xp: " + stat.getXp());
		if (stat.getLevel() != skills.get(stat.getSkill())) {
			skills.put(stat.getSkill(), stat.getLevel());  //TODO add a check in config to see if this levelup is worth broadcasting
			//TODO enqueue on the event queue that the skill leveled up, possible sanity check if increasing?
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varb) {
		if (uncompletedQuests == null || uncompletedQuests.isEmpty()) {
			return;
		}
		varbitChangedQuestsNeedToBeChecked = true;
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath) {
		if (runescapeAccountSession == null) {
			return;
		}

		Actor actor = actorDeath.getActor();

		if (!(actor instanceof Player)) {
			return;
		}

		Player player = (Player) actor;

		if (player != client.getLocalPlayer()) {
			return;
		}

		WorldPoint point = player.getWorldLocation();

		PlayerDeath death = new PlayerDeath(
			runescapeAccountSession.getAccountHash(),
			point.getX(),
			point.getY(),
			point.getPlane(),
			client.getWorld()
		);

		runescapeAccountSession.getActivityUpdateBroadcaster().broadcastActivityUpdate(death);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
		if (widgetLoaded.getGroupId() != WidgetID.WORLD_MAP_GROUP_ID) {
			return;
		}

		if (runescapeAccountSession == null) {
			return;
		}

		runescapeAccountSession.setWorldMapIsShowing(true);
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed widgetClosed) {
		if (widgetClosed.getGroupId() != WidgetID.WORLD_MAP_GROUP_ID) {
			return;
		}

		if (runescapeAccountSession == null) {
			return;
		}

		runescapeAccountSession.setWorldMapIsShowing(false);
	}


	@Subscribe
	public void onGameTick(GameTick tick) {
			tickcounter++;

			if (needsToCreateRunescapeAccountSession && accountSession != null) {
				needsToCreateRunescapeAccountSession = false;

				accountSession.startRunescapeAccountSession(
					Long.toString(client.getAccountHash()),
					client.getLocalPlayer().getName(),
					runescapeAccountSession -> {
						this.runescapeAccountSession = runescapeAccountSession;
						runescapeAccountSession.setListener(this);
					},
					gson,
					worldMapManager
				);
			}

			if (shouldBroadcastLocation()) {
				sendLocationUpdate();
			}

			if (!skillsLoaded) {
				Skill[] allSkills = Skill.values();

				Map<Skill, Integer> skills = new HashMap<>();

				for (Skill skill : allSkills) {
					if (skill == Skill.OVERALL) {
						continue;
					}
					skills.put(skill, client.getRealSkillLevel(skill));
				}

				this.skills = skills;
				skillsLoaded = true;
			}

			if (uncompletedQuests == null) {
				long startTime = System.currentTimeMillis();
				uncompletedQuests = new ArrayList<>();
				for(Quest q : Quest.values()) {
					if(getState(q.getId()) != QuestState.FINISHED) {
						uncompletedQuests.add(q);
					}
				}
				long endTime = System.currentTimeMillis();
				long duration = (endTime - startTime);
				System.out.println("Checked all quests - " + duration + "ms");
			}

			if (varbitChangedQuestsNeedToBeChecked) {
				long startTime = System.currentTimeMillis();
				List<Quest> newlyCompletedQuests = uncompletedQuests
						.stream()
						.filter(q -> getState(q.getId()).equals(QuestState.FINISHED))
						.collect(Collectors.toList());

				//TODO notify server of completed quest(s)
				uncompletedQuests.removeAll(newlyCompletedQuests);
				varbitChangedQuestsNeedToBeChecked = false;

				long endTime = System.currentTimeMillis();
				long duration = (endTime - startTime);
				System.out.println("Checked on varbit change - " + duration + "ms");
			}


//			removeWaypoints();
//			setWaypoints();

	}

	@Provides
	FriendMonitorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FriendMonitorConfig.class);
	}

	public boolean isPvpWorld()
	{
		return WorldType.isPvpWorld(client.getWorldType());
	}

	public boolean isWilderness()
	{
		return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
	}

	public boolean shouldBroadcastLocation() {
		return !isPvpWorld() && !isWilderness();// && (client.getVarbitValue(FRIEND_CHAT_STATUS_VARBIT) == 2 && config.getPrivacyModeEnabled()); // varbit 13674 contains private chat status, 0=on, 1=friends, 2=off
	}

	public QuestState getState(Integer id) {
		client.runScript(ScriptID.QUEST_STATUS_GET, id);
		switch (client.getIntStack()[0]) {
			case 2:
				return QuestState.FINISHED;
			case 1:
				return QuestState.NOT_STARTED;
			default:
				return QuestState.IN_PROGRESS;
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened menu) {

	}

	public void sendLocationUpdate() {
		WorldPoint wp = client.getLocalPlayer().getWorldLocation();
		ActivityUpdate loc = new Location(wp.getX(), wp.getY(), wp.getPlane(), client.getWorld(), Long.toString(client.getAccountHash()));

		if (runescapeAccountSession == null) {
			return;
		}

		runescapeAccountSession.getActivityUpdateBroadcaster().broadcastActivityUpdate(loc);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if ((event.getType() != MenuAction.CC_OP.getId() && event.getType() != MenuAction.CC_OP_LOW_PRIORITY.getId()))
		{
			return;
		}

		final String option = event.getOption();
		final int componentId = event.getActionParam1();
		final int groupId = WidgetInfo.TO_GROUP(componentId);

		//TODO decide whether to keep all these. Only first one is required for friend list.
		if (groupId == WidgetInfo.FRIENDS_LIST.getGroupId() && option.equals("Delete")
				|| groupId == WidgetInfo.FRIENDS_CHAT.getGroupId() && (option.equals("Add ignore") || option.equals("Remove friend"))
				|| (componentId == WidgetInfo.CLAN_MEMBER_LIST.getId() || componentId == WidgetInfo.CLAN_GUEST_MEMBER_LIST.getId()) && (option.equals("Add ignore") || option.equals("Remove friend"))
				|| groupId == WidgetInfo.PRIVATE_CHAT_MESSAGE.getGroupId() && (option.equals("Add ignore") || option.equals("Message"))
				|| groupId == WidgetID.GROUP_IRON_GROUP_ID && (option.equals("Add friend") || option.equals("Remove friend") || option.equals("Remove ignore"))
		)
		{
			client.createMenuEntry(-2)
					.setOption(GET_ACTIVITY)
					.setTarget(event.getTarget())
					.setType(MenuAction.RUNELITE)
					.setIdentifier(event.getIdentifier())
					.onClick(e ->
					{
						// TODO create request for history, if not in valid friends then add to pending friends
						/*HiscoreEndpoint endpoint = findHiscoreEndpointFromPlayerName(e.getTarget());
						String target = Text.removeTags(e.getTarget());
						lookupPlayer(target, endpoint);*/
					});
		}
	}

	@Override
	public void onLoggedIn(AccountSession session) {
		this.accountSession = session;
	}

	@Override
	public void onLoginFailed() {

	}

	@Override
	public void invokeOnClientThread(Runnable r) {
		clientThread.invoke(r);
	}

	@Override
	public void friendDied(FriendDeathMessage message) {
		clientThread.invoke(() -> {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message.getDisplayName() + " has died.", null);
		});
	}
}
