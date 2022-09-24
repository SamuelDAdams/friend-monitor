package com.friendmonitor;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetID;
import okhttp3.*;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Friend Monitor"
)
public class FriendMonitorPlugin extends Plugin implements ConnectionListener, AuthenticationClientListener
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

	@Getter
	@Setter
	private Map<Skill, Integer> skills;

	@Getter
	@Setter
	private List<Quest> uncompletedQuests;

	private ConnectionHandler connectionHandler;

	@Inject
	private Client client;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private FriendMonitorConfig config;

	@Inject
	private AuthenticationClient authenticationClient;

	private AccountSession accountSession;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN && connectionHandler == null)
		{
			Long accountHash = client.getAccountHash();
			System.out.println(accountHash);

			connectionHandler = new ConnectionHandler(accountHash, this);

			try {
				authenticationClient.setListener(this);
				authenticationClient.login();
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			skillsLoaded = false;
			skills.clear();
			uncompletedQuests.clear();
			uncompletedQuests = null;
			connectionHandler.close();
			connectionHandler = null;
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
	public void onGameTick(GameTick tick) {
			tickcounter++;
			if (tickcounter >= 4 && shouldBroadcast()) {
				tickcounter = 0;
				String playerName = client.getLocalPlayer().getName();
				int playerWorld = client.getWorld();
				WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();

//				System.out.println("Player position x: " + playerPos.getX() + " y: " + playerPos.getY());
//				System.out.println("Player plane: " + playerPos.getPlane());
				//check for new events that need to be posted that are currently in the queue
				//send update with position and new events

				//LiveLocationSharingData d = new LiveLocationSharingData(playerName, playerPos.getX(), playerPos.getY(), playerPos.getPlane(), playerType, playerTitle, playerWorld); //Custom data type for what we need
				//dataManager.makePostRequest(d); //send through socket
				//log.info(String.format("x: %s, y: %s, plane: %s", playerPos.getX(), playerPos.getY(), playerPos.getPlane()));

				//request events from friends
				//write events to chatb
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

			if(uncompletedQuests == null) {
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

			if(varbitChangedQuestsNeedToBeChecked) {
				long startTime = System.currentTimeMillis();
				List<Quest> newlyCompletedQuests = uncompletedQuests.stream().filter(q -> getState(q.getId()).equals(QuestState.FINISHED)).collect(Collectors.toList());
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

	public boolean shouldBroadcast() {
		return !isPvpWorld() && !isWilderness() && (client.getVarbitValue(FRIEND_CHAT_STATUS_VARBIT) == 2 && config.getPrivacyModeEnabled()); // varbit 13674 contains private chat status, 0=on, 1=friends, 2=off
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

	public void onMessage(String message) {
		System.out.println(message);
	}

	@Override
	public void onLoggedIn(String idToken, String refreshToken) {
		accountSession = new AccountSession(httpClient, idToken, refreshToken);

		Request r = new Request.Builder()
				.url("https://localhost:7223/")
				.build();

		accountSession.getHttpClient().newCall(r).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				System.out.println(response);
			}
		});
	}

	@Override
	public void onLoginFailed() {

	}
}
