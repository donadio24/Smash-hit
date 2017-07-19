package network;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

import GameGui.GameManager;
import GameGui.SoundManager;
import GameGui.HUD.MultiplayerHUD;
import GameGui.Screen.GameOverScreen;
import GameGui.Screen.PauseScreen;
import network.packet.MovePacket;
import videogame.Countdown;
import videogame.GameConfig;

public class MultiplayerScreen implements Screen 
{
	private GameManager game;
	protected Camera cam;
	protected ModelBatch batch;
	public static ModelInstance playerInstance;
	protected Environment environment;
	protected MultiplayerWorld world;
	protected int degrees = 90;
	protected long hitTime = 0;
	protected long startTime = 0;
	protected boolean hitAnimation = false;
	private MultiplayerHUD hud;
	protected AnimationController playerController;
	protected ArrayList<AnimationController> destroyedController;
	protected ArrayList<AnimationController> coinController;
	protected String[] state = new String[4];
	private Socket socket;
	private ClientThread client;

	protected Controller joystick;
//	private final int A = 0;
	private final int B = 1;
//	private final int Y = 3;
//	private final int X = 2;
	private final int LB = 4;
	private final int RB = 5;
    private final int START = 7;
	private boolean PAUSE = false;
	private final int AXIS_LY = 0; //-1 is up | +1 is down
	private final int AXIS_LX = 1; //-1 is left | +1 is right
	private final int AXIS_RY = 2; //-1 is up | +1 is down
	private final int AXIS_RX = 3; //-1 is left | +1 is right

	public MultiplayerScreen() { }
		
	public MultiplayerScreen(GameManager _game, String ip, int port)
	{
		try {
			socket = new Socket(ip, port);
			client = new ClientThread(socket);
			client.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.game = _game;
		
		for (Controller c : Controllers.getControllers()) 
		{
			if(c.getName().contains("XBOX") && c.getName().contains("360")) 
				joystick = c;
		}
		
		if(joystick != null)
		{
			System.out.println("joystick connected");
			initJoystick();
		}

		SoundManager.gameSoundtrack.play();
		
		world = new MultiplayerWorld();
		batch = new ModelBatch();

		game.mapGenerator.assets.loadPlayer();
		initCamera();
		initEnvironment();
		initAnimation();

		game.countdown.pause = false;
		hud = new MultiplayerHUD();
	}

	private void initJoystick()
	{
		joystick.addListener(new ControllerAdapter()
		{
			@Override
			public boolean buttonDown(Controller controller, int buttonIndex)
			{
				if(buttonIndex == B)		// B
			{
				GameConfig.HIT = true;
				if(GameConfig.STATE.equals("hit"))
				{
//						SoundManager.hitSound.play(SoundManager.soundVolume);
					hitAnimation = true;
					hitTime = System.currentTimeMillis();
				}
			}
			else if(buttonIndex == RB) //&& GameConfig.STATE !="tornado"
			{
				GameConfig.stateIndex++;
				GameConfig.stateIndex %= 4;
			
				GameConfig.STATE = state[GameConfig.stateIndex];
			}
			else if(buttonIndex == LB) //&& GameConfig.STATE !="tornado"
			{
				GameConfig.stateIndex--;
				
				if(GameConfig.stateIndex < 0)
					GameConfig.stateIndex = 3;
				
				GameConfig.STATE = state[GameConfig.stateIndex];
			}
			else if(buttonIndex == START)		// START
				PAUSE = true;
			return true;
		}

		@Override
		public boolean axisMoved(Controller controller, int axisIndex, float value)
		{
		//			if(axisIndex == AXIS_RX)// && value == -1)
		//				GameConfig.LEFT = true;
		//			if(axisIndex == AXIS_RX)// && value == 1)
		//				GameConfig.RIGHT = true;
		//			if(axisIndex == AXIS_RY)// && value == -1)
		//				GameConfig.ON = true;
		//			if(axisIndex == AXIS_RY )//&& value == 1)
		//				GameConfig.BACK = true;
			
			if(axisIndex == AXIS_LX && value == -1)
			{
				cam.direction.rotate(4,0,1,0);
				degrees += 4;
			}
			if(axisIndex == AXIS_LX && value == 1)
			{
				cam.direction.rotate(-4,0,1,0);
				degrees -= 4;
			}
			
			return true;
		}
	});
	
}

	protected void initAnimation()
	{
		destroyedController = new ArrayList<AnimationController>();
		coinController = new ArrayList<AnimationController>();
		
		playerController = new AnimationController(playerInstance);
		playerController.setAnimation("Armature|ArmatureAction",-1);
	}

	protected void initCamera() 
	{
		cam = new PerspectiveCamera(67, GameConfig.Screen_Width, GameConfig.Screen_Height);
		cam.position.set(GameConfig.player.getPosition());
		cam.lookAt(GameConfig.DIRECTION);
		cam.near = 1f;
		cam.far = 1500f;
		cam.direction.x += 0.4f;
		cam.direction.y -= 0.5f;
		cam.direction.z -= 0.4f;
		cam.update();
	}

	protected void initEnvironment() 
	{
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
	}

	public void handleInput() 
	{
		if (Gdx.input.isKeyPressed(Input.Keys.A))
		{
			client.out.println(new MovePacket(GameConfig.DIRECTION,"left").toString());
			GameConfig.LEFT = true;
		}
		else if (Gdx.input.isKeyPressed(Input.Keys.D))
		{
			client.out.println(new MovePacket(GameConfig.DIRECTION,"right").toString());
			GameConfig.RIGHT = true;
		}
		else if (Gdx.input.isKeyPressed(Input.Keys.W))
		{
			client.out.println(new MovePacket(GameConfig.DIRECTION,"on").toString());
			GameConfig.ON = true;
		}
		else if (Gdx.input.isKeyPressed(Input.Keys.S))
		{
			client.out.println(new MovePacket(GameConfig.DIRECTION,"back").toString());
			GameConfig.BACK = true;		
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
		{
			client.out.println(new MovePacket(GameConfig.DIRECTION,"LEFT").toString());
			cam.direction.rotate(4,0,1,0);
			degrees += 4;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
		{
			client.out.println(new MovePacket(GameConfig.DIRECTION,"RIGHT").toString());
			cam.direction.rotate(-4,0,1,0);
			degrees -= 4;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE))
		{
			GameConfig.HIT = true;
			hitAnimation = true;
			hitTime = System.currentTimeMillis();
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
		{
			PAUSE = true;
		}
	}
	
	public void render(float delta) 
	{
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		
		if(!client.receive.equals(""))
		{
			String[] packets = client.receive.split(","); 
			if(packets[0].equals("load"))
			{
				if(!GameConfig.isServer)
					game.mapGenerator.loadRoom(packets[1]);
			}
			else if(packets[0].equals("ready"))
			{
				world.addPlayers(packets);
				MultiplayerLobby.ready = true;
			}
			else
				world.packetManager(packets, delta);
		}

		if(joystick == null)
			handleInput();

		if(PAUSE)
		{
			PAUSE = false;
			game.countdown.pause = true;
			game.setScreen(new PauseScreen(game, this));
		}

		addAnimation();
		handleAnimation();
		handleSound();
		world.update(delta);

		// move player instance
		playerInstance.transform.setToTranslation(GameConfig.player.getPosition());
		playerInstance.transform.rotate(0,1,0,degrees);
		
		// camera update
		cam.position.set(GameConfig.player.getPosition().cpy().add(0,7,0));
		
		cam.update();
		GameConfig.DIRECTION = cam.direction;

		batch.begin(cam);

		// render player instance
		batch.render(playerInstance, environment);
		
		// render walls
		synchronized (GameConfig.wallsInstance)
		{
			for (final ModelInstance wall : GameConfig.wallsInstance)
				batch.render(wall, environment);
		}
		
		// render tools instance
		synchronized (GameConfig.toolsInstance)
		{
			for(final ModelInstance mod : GameConfig.toolsInstance.get(GameConfig.actualLevel-1))
				batch.render(mod, environment);

			// render next room's model instances
			if(GameConfig.actualLevel < GameConfig.toolsInstance.size())
			{
				for(final ModelInstance mod : GameConfig.toolsInstance.get(GameConfig.actualLevel))
					batch.render(mod, environment);
			}
		}
		
		// render destroyed tools
		for (final ModelInstance instance : GameConfig.destroyed)
			batch.render(instance, environment);
		
		// render coins
		for (final ModelInstance instance : GameConfig.coins)
			batch.render(instance, environment);
		
		batch.end();

		// update and render hud
		hud.update();
		hud.stage.act();
		hud.stage.draw();
	
		if(GameConfig.GAME_OVER)
		{
			synchronized (game.countdown)
			{
				game.countdown.pause = true;
			}
			synchronized(game.mapGenerator)
			{
				game.mapGenerator.pause = true;
			}
			this.dispose();
			game.setScreen(new GameOverScreen(game));
		}
	}

	protected void addAnimation()
	{
		for(int i = destroyedController.size(); i < GameConfig.destroyed.size(); i++)
		{
			destroyedController.add(new AnimationController(GameConfig.destroyed.get(i)));
			destroyedController.get(i).setAnimation("Armature|ArmatureAction");
		}
		
		for(int i = coinController.size(); i < GameConfig.coins.size(); i++)
		{
			coinController.add(new AnimationController(GameConfig.coins.get(i)));
			coinController.get(i).setAnimation("Armature|ArmatureAction");
		}
	}
	protected void handleSound()
	{
		if(hitAnimation)
		{
			if(GameConfig.HIT && GameConfig.hitted)
			{
				SoundManager.hitSound.play();
			}
			else if(!GameConfig.HIT)
			{
				SoundManager.hitSound.stop();
				GameConfig.hitted = false;
			}
		}
	}

	protected void handleAnimation()
	{
		synchronized (game.mapGenerator.assets.clockAnimation)
		{
			for (AnimationController clock : game.mapGenerator.assets.clockAnimation)
				clock.update(Gdx.graphics.getDeltaTime());
		}
		
		if(!hitAnimation && (GameConfig.ON || GameConfig.BACK || GameConfig.RIGHT || GameConfig.LEFT))
		{
			playerController.setAnimation("Armature|ArmatureAction",-1);
			playerController.update(Gdx.graphics.getDeltaTime());
		}

		if(hitAnimation)
		{
			playerController.setAnimation("Armature|hit",-1);
			playerController.update(Gdx.graphics.getDeltaTime());
		}

		if(hitAnimation && System.currentTimeMillis()-hitTime > 400)
		{
			playerController.setAnimation("Armature|ArmatureAction",-1);
			playerController.update(Gdx.graphics.getDeltaTime());
			hitAnimation = false;
		}
		
		for (AnimationController controller : destroyedController)
			controller.update(Gdx.graphics.getDeltaTime());
		
		for (AnimationController Controller : coinController)
			Controller.update(Gdx.graphics.getDeltaTime());
		
		if(Countdown.getTime() %10 == 0)
		{
			destroyedController.clear();
			GameConfig.destroyed.clear();
			
			coinController.clear();
			GameConfig.coins.clear();
		}
	}

	public void dispose()
	{
		GameConfig.tools.clear();
		GameConfig.toolsInstance.clear();
		batch.dispose();
		hud.dispose();
	}
	
	@Override
	public void show() { }

	@Override
	public void resize(int width, int height)
	{
		hud.resize(width,height);
		game.options.putInteger("screen_width", width);
		game.options.putInteger("screen_height", height);
		game.options.flush();
		
		GameConfig.Screen_Height = height;
		GameConfig.Screen_Width = width;
	}

	@Override
	public void pause() { }

	@Override
	public void resume() { }

	@Override
	public void hide() { }
}