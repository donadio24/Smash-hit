package GameGui.Screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
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
import GameGui.Hud;
import videogame.AI.Dijkstra;
import videogame.Countdown;
import videogame.GameConfig;
import videogame.AI.Vertex;
import videogame.bonus.Bomb;
import videogame.World;

public class GameScreen implements Screen 
{
	private GameManager game;
	private Camera cam;
	private ModelBatch batch;
	public static ModelInstance playerInstance;
	public ModelInstance bombInstance;
	private ArrayList<ModelInstance> hints;
	private Environment environment;
	private World world;
	private int degrees = 90;
	private Hud hud;
	private AnimationController playerController;
	private ArrayList<AnimationController> destroyedController;
	private ArrayList<AnimationController> coinController;
	private int stateIndex = 0;
	private String[] state = new String[3];
	
	public GameScreen(GameManager _game)
	{
		game = _game;
		
		state[0] = "hit";
		state[1] = "bomb";
		state[2] = "tornado";
		
		GameConfig.gameSoundtrack.play();
		GameConfig.gameSoundtrack.setVolume(GameConfig.volume);
		
		world = new World();
		batch = new ModelBatch();

		game.mapGenerator.assets.loadPlayer();
		initCamera();
		initEnvironment();
		initAnimation();
		
		bombInstance = new ModelInstance(game.mapGenerator.assets.bomb);
		
		game.countdown.pause = false;
		hud = new Hud();
	}

	private void initAnimation()
	{
		destroyedController = new ArrayList<AnimationController>();
		coinController = new ArrayList<AnimationController>();
		
		playerController = new AnimationController(playerInstance);
		playerController.setAnimation("Armature|ArmatureAction",-1);
	}

	private void initCamera() 
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

	private void initEnvironment() 
	{
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		hints = new ArrayList<ModelInstance>();
	}

	public void handleInput() 
	{
		if (Gdx.input.isKeyPressed(Input.Keys.A))
			GameConfig.LEFT = true;
		else if (Gdx.input.isKeyPressed(Input.Keys.D))
			GameConfig.RIGHT = true;
		else  if (Gdx.input.isKeyPressed(Input.Keys.W))
			GameConfig.ON = true;
		else if (Gdx.input.isKeyPressed(Input.Keys.S))
			GameConfig.BACK = true;		
		if (Gdx.input.isKeyJustPressed(Input.Keys.F))
			help();
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
		{
			cam.direction.rotate(2,0,1,0);
			degrees += 2;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
		{
			cam.direction.rotate(-2,0,1,0);
			degrees -= 2;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE))
		{
			GameConfig.HIT = true;
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
		{
			game.countdown.pause = true;
			game.setScreen(new PauseScreen(game, this));
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT))
		{
			stateIndex++;
			stateIndex %= 3;
			
			GameConfig.STATE = state[stateIndex];
		}
	}
		
	public void render(float delta) 
	{
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		handleInput();
		addAnimation();
		handleAnimation();
		world.update(delta);

		// move player instance
		playerInstance.transform.setToTranslation(GameConfig.player.getPosition());
		playerInstance.transform.rotate(0,1,0,degrees);
		
		// camera update
		cam.position.set(GameConfig.player.getPosition().cpy().add(0,6.5f,0));
		
		cam.update();
		GameConfig.DIRECTION = cam.direction;

		batch.begin(cam);

		// render player instance
		batch.render(playerInstance, environment);
		
		// render hints
		for(final ModelInstance mod : hints)
			batch.render(mod, environment);
		
		ModelInstance model = new ModelInstance(game.mapGenerator.assets.grid);
		model.transform.setToTranslation(41f,-4.8f,41.5f);
		batch.render(model, environment);
		
		ModelInstance model2 = new ModelInstance(game.mapGenerator.assets.grid);
		model2.transform.setToTranslation(131,-4.8f,41.5f);
		batch.render(model2, environment);
		
		// render walls
		synchronized (GameConfig.wallsInstance)
		{
			for (final ModelInstance wall : GameConfig.wallsInstance)
				batch.render(wall, environment);
		}
		
		if(world.getBomb() instanceof Bomb)
		{
			System.out.println("bomb");
			bombInstance.transform.setToTranslation(world.getBomb().getPosition());
			batch.render(bombInstance, environment);
		}
		
		// render tools instance
		synchronized (GameConfig.toolsInstance)
		{
			for(final ModelInstance mod : GameConfig.toolsInstance.get(GameConfig.actualLevel-1))
				batch.render(mod, environment);
	
			// render next room's model instances
//			if(GameConfig.level > GameConfig.actualLevel+1 )
//			{
//				for(final ModelInstance mod : GameConfig.toolsInstance.get(GameConfig.actualLevel))
//					batch.render(mod, environment);
//			}
		}
		
		// render destroyed tools
		for (final ModelInstance instance : GameConfig.destroyed)
			batch.render(instance, environment);
		
		// render coins
		for (final ModelInstance instance : GameConfig.coins)
			batch.render(instance, environment);
		
		batch.end();

		// update and render hud
		hud.update(delta);
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

	private void addAnimation()
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

	private void handleAnimation()
	{
		synchronized (game.mapGenerator.assets.clockAnimation)
		{
			for (AnimationController clock : game.mapGenerator.assets.clockAnimation)
				clock.update(Gdx.graphics.getDeltaTime());
		}
		
		if(state[stateIndex] == "tornado")
		{
			playerController.setAnimation("Armature|bonus",-1);
			playerController.update(Gdx.graphics.getDeltaTime());
		}
		
		if((GameConfig.ON || GameConfig.BACK || GameConfig.RIGHT || GameConfig.LEFT))
		{
			
			if(state[stateIndex] != "tornado")
			{
				playerController.setAnimation("Armature|ArmatureAction",-1);
				playerController.update(Gdx.graphics.getDeltaTime());
			}
		}

		if(GameConfig.HIT)
		{
			if(state[stateIndex] != "tornado")
			{
				playerController.setAnimation("Armature|"+state[stateIndex],-1);
				playerController.update(Gdx.graphics.getDeltaTime());
			}
		}
		
		for (AnimationController controller : destroyedController)
			controller.update(Gdx.graphics.getDeltaTime());
		
		for (AnimationController Controller : coinController)
			Controller.update(Gdx.graphics.getDeltaTime());
		
		if(Countdown.getTime() %5 == 0)
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
//		game.mapGenerator.assets.dispose();
		hud.dispose();
	}

	private void help()
	{
		hints.clear();
		
		// apply dijkstra
        List<Vertex> path = Dijkstra.getShortestPath();
        
        //	create hints model
        float position = (GameConfig.actualLevel-1) * 5.5f * GameConfig.ROOM_ROW;
        for (Vertex vertex : path)
        {
        	ModelInstance mod = new ModelInstance(game.mapGenerator.assets.help);
        	mod.transform.setToTranslation(-2+vertex.x * GameConfig.CELL_HEIGHT +position, -4.7f, 1 + vertex.y * GameConfig.CELL_WIDTH);
        	hints.add(mod);
		}
     }
	
	@Override
	public void show() { }

	@Override
	public void resize(int width, int height)
	{
//		game.options.putInteger("screen_width", width);
//		game.options.putInteger("screen_height", height);
//		game.options.flush();
		
		GameConfig.Screen_Height = height;
		GameConfig.Screen_Width = width;
		
//		System.out.println("width "+GameConfig.Screen_Width + "   height "+GameConfig.Screen_Height);
	}

	@Override
	public void pause() { }

	@Override
	public void resume() { }

	@Override
	public void hide() { }
}
