package network.Screen;

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
import com.badlogic.gdx.controllers.PovDirection;
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
import GameGui.Screen.PauseScreen;
import network.MultiplayerHUD;
import network.MultiplayerWorld;
import network.packets.AnimationPacket;
import network.packets.MovePacket;
import network.threads.ClientThread;
import videogame.Countdown;
import videogame.GameConfig;

public class MultiplayerScreen implements Screen 
{
	public GameManager game;
	private Camera cam;
	private ModelBatch batch;
	private Environment environment;
	private MultiplayerWorld world;
	private ArrayList<Long> hitTimes;
	private ArrayList<Boolean> hitAnimations;
	private MultiplayerHUD hud;
	private ArrayList<AnimationController> playerControllers;
	private ArrayList<AnimationController> destroyedController;
	private Socket socket;
	public static ClientThread client;

	private Controller joystick;

	//pressing button boolean
	private boolean keyB = false;
	private boolean keyLB = false;
	private boolean keyRB = false;
	private boolean keyStart = false;
	
	//movement analog boolean
	private boolean moveOn = false;
	private boolean moveBack = false;
	private boolean moveLeft = false;
	private boolean moveRight = false;
	
	private final int B = 1;
	private final int LB = 4;
	private final int RB = 5;
	private final int START = 7;

	public MultiplayerScreen() { }
	public MultiplayerWorld getWorld() {return world;}
	
	public MultiplayerScreen(GameManager _game, String ip, int port)
	{
		this.game = _game;

		playerControllers = new ArrayList<AnimationController>();
		for (Controller c : Controllers.getControllers()) 
		{
			if(c.getName().contains("XBOX") && c.getName().contains("360")) 
				joystick = c;
		}
		
		if(joystick != null)
		{
			initJoystick();
		}

		SoundManager.gameSoundtrack.play();
		batch = new ModelBatch();

		initCamera();
		initEnvironment();

		hud = new MultiplayerHUD();
		
		try
		{
			socket = new Socket(ip, port);
			client = new ClientThread(socket, this);
			client.start();
		}
		catch (UnknownHostException e)
		{	e.printStackTrace(); }
		catch (IOException e)
		{	e.printStackTrace(); }
		world = new MultiplayerWorld(client);
	}

	private void initJoystick() 
	{
		joystick.addListener(new ControllerAdapter() 
		{			
			@Override
			public boolean buttonDown(Controller controller, int buttonIndex) 
			{
				if (buttonIndex == B)
					keyB = true;
				else if (buttonIndex == RB)
					keyRB = true;
				else if (buttonIndex == LB)
					keyLB = true;
				else if (buttonIndex == START)
					keyStart = true;
				return true;
			}
			
			@Override
			public boolean buttonUp(Controller controller, int buttonIndex) 
			{
				if (buttonIndex == B)
					keyB = false;
				else if (buttonIndex == RB)
					keyRB = false;
				else if (buttonIndex == LB)
					keyLB = false;
				else if (buttonIndex == START)
					keyStart = false;
				return true;
			}
			@Override
			public boolean povMoved(Controller controller, int povIndex, PovDirection value)
			{
				if(value == PovDirection.north)
					moveOn = true;
				else
					moveOn = false;
				
				if(value == PovDirection.south)
					moveBack = true;
				else
					moveBack = false;
				
				if(value == PovDirection.east)
					moveRight = true;
				else 
					moveRight = false;
				
				if(value == PovDirection.west)
					moveLeft = true;
				else 
					moveLeft = false;
				
				return true;
			}
		});
	}
	
	public void initAnimation()
	{
		hitTimes = new ArrayList<Long>();
		hitAnimations = new ArrayList<Boolean>();
		destroyedController = new ArrayList<AnimationController>();
		playerControllers = new ArrayList<AnimationController>();
		
		for(int i = 0; i < GameConfig.playersInstance.size(); i++)
		{
			playerControllers.add(new AnimationController(GameConfig.playersInstance.get(i)));
			playerControllers.get(i).setAnimation("Armature|ArmatureAction",-1);
			hitAnimations.add(false);
			hitTimes.add(0l);
		}
	}

	private void initCamera() 
	{
		cam = new PerspectiveCamera(67, GameConfig.Screen_Width, GameConfig.Screen_Height);
		cam.lookAt(GameConfig.DIRECTION);
		cam.near = 1f;
		cam.far = 1500f;
		cam.direction.x += 0.4f;
		cam.direction.y -= 0.5f;
		cam.direction.z -= 0.4f;
		cam.update();
	}

	private void initEnvironment() 
	{
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
	}

	public void handleInput() 
	{
		if (Gdx.input.isKeyPressed(Input.Keys.A) || moveLeft)
			GameConfig.LEFT = true;
		else if (Gdx.input.isKeyPressed(Input.Keys.D) || moveRight)
			GameConfig.RIGHT = true;
		else if (Gdx.input.isKeyPressed(Input.Keys.W) || moveOn)
			GameConfig.ON = true;
		else if (Gdx.input.isKeyPressed(Input.Keys.S) || moveBack)
			GameConfig.BACK = true;		
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || keyLB)
		{
			cam.direction.rotate(4,0,1,0);
			GameConfig.players.get(GameConfig.ID).angle += 4;
			client.out.println(new MovePacket(GameConfig.players.get(GameConfig.ID).getPosition(), GameConfig.players.get(GameConfig.ID).angle));
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || keyRB)
		{
			cam.direction.rotate(-4,0,1,0);
			GameConfig.players.get(GameConfig.ID).angle -= 4;
			client.out.println(new MovePacket(GameConfig.players.get(GameConfig.ID).getPosition(),GameConfig.players.get(GameConfig.ID).angle));
		}
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE) || keyB)
		{
			GameConfig.HIT = true;
			hitAnimations.set(GameConfig.ID,true);
			hitTimes.set(GameConfig.ID, System.currentTimeMillis());
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || keyStart)
		{
			keyStart = false;
			game.setScreen(new PauseScreen(game, this));
		}
	}
	
	public void render(float delta) 
	{
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		
		handleInput();

		addAnimation();
		handleAnimation();
		world.update(delta);

		// render player instance
		for(int i = 0; i < GameConfig.playersInstance.size(); i++)
		{
			GameConfig.playersInstance.get(i).transform.setToTranslation(GameConfig.players.get(i).getPosition());
			GameConfig.playersInstance.get(i).transform.rotate(0,1,0,90+GameConfig.players.get(i).angle);
			batch.render(GameConfig.playersInstance.get(i), environment);
		}
		
		// camera update
		cam.position.set(GameConfig.players.get(GameConfig.ID).getPosition().cpy().add(0,7,0));
		
		cam.update();
		GameConfig.DIRECTION = cam.direction;

		batch.begin(cam);

		// render walls
		synchronized (GameConfig.wallsInstance)
		{
			for (final ModelInstance wall : GameConfig.wallsInstance)
				batch.render(wall, environment);
		}
		
		// render tools instance
		for(final ModelInstance mod : GameConfig.multiplayerInstances)
			batch.render(mod, environment);

//		render destroyed tools
		for (final ModelInstance instance : GameConfig.destroyed)
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
				game.countdown.active = false;
			}

			this.dispose();
			game.setScreen(new MultiplayerOverScreen(game));
		}
	}

	private void addAnimation()
	{
		for(int i = destroyedController.size(); i < GameConfig.destroyed.size(); i++)
		{
			destroyedController.add(new AnimationController(GameConfig.destroyed.get(i)));
			destroyedController.get(i).setAnimation("Armature|ArmatureAction");
		}
	}

	private void handleAnimation()
	{
		if(hitAnimations.get(GameConfig.ID) && (GameConfig.ON || GameConfig.BACK || GameConfig.RIGHT || GameConfig.LEFT))
		{
			client.out.println(new AnimationPacket("move"));
			playerControllers.get(GameConfig.ID).setAnimation("Armature|ArmatureAction",-1);
			playerControllers.get(GameConfig.ID).update(Gdx.graphics.getDeltaTime());
		}

		if(hitAnimations.get(GameConfig.ID))
		{
			client.out.println(new AnimationPacket("hit"));
			playerControllers.get(GameConfig.ID).setAnimation("Armature|hit",-1);
			playerControllers.get(GameConfig.ID).update(Gdx.graphics.getDeltaTime());
		}

		for(int i = 0; i < hitAnimations.size(); i++)
		{
			if(hitAnimations.get(i) && System.currentTimeMillis() - hitTimes.get(i) > 400)
			{
				client.out.println(new AnimationPacket("endHit"));
				playerControllers.get(GameConfig.ID).setAnimation("Armature|ArmatureAction",-1);
				playerControllers.get(GameConfig.ID).update(Gdx.graphics.getDeltaTime());
				hitAnimations.set(i, false);
			}
		}
		
		for (AnimationController controller : destroyedController)
			controller.update(Gdx.graphics.getDeltaTime());
		
		if(Countdown.getTime() %10 == 0)
		{
			destroyedController.clear();
			GameConfig.destroyed.clear();
		}
	}

	public void handlePlayerAnimations(int id, String type)
	{
		if(type.equals("hit"))
		{
			playerControllers.get(id).setAnimation("Armature|hit",-1);
			playerControllers.get(id).update(Gdx.graphics.getDeltaTime());
			hitAnimations.set(id, true);
		}
		else if(type.equals("move"))
		{
			playerControllers.get(id).setAnimation("Armature|ArmatureAction",-1);
			playerControllers.get(id).update(Gdx.graphics.getDeltaTime());
		}
		else if(type.equals("endHit"))
		{
			playerControllers.get(id).setAnimation("Armature|ArmatureAction",-1);
			playerControllers.get(id).update(Gdx.graphics.getDeltaTime());
			hitAnimations.set(id, false);
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