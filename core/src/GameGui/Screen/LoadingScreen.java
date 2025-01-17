package GameGui.Screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import GameGui.GameManager;
import GameGui.SoundManager;
import network.Screen.MultiplayerScreen;
import videogame.GameConfig;

public class LoadingScreen implements Screen
{
	private GameManager game;
	private MultiplayerScreen multiplayerScreen;
	
	private SpriteBatch spriteBatch;
	private Texture backgroundSingle;
	private Texture backgroundMulti;
	private Texture loadingBar; 
	
	private float progress;
	
	public LoadingScreen(GameManager game, MultiplayerScreen _multiplayerScreen)
	{
		this.game = game;
		this.multiplayerScreen = _multiplayerScreen;
		init();
	}
	
	public LoadingScreen(GameManager _game)
	{
		game = _game;
		init();
	}
	
	public void init()	
	{
		SoundManager.menuSoundtrack.stop();
		
		spriteBatch = new SpriteBatch();
        backgroundSingle = new Texture(Gdx.files.internal("texture/loading_background_single.png"));
        backgroundMulti = new Texture(Gdx.files.internal("texture/loading_background_multi.png"));
        loadingBar = new Texture(Gdx.files.internal("loading_bar/bate_0.png"));
	
        if(GameConfig.MULTIPLAYER)
        {
	       	game.multiplayerMapGenerator.assets.loadPlayer();
	       	multiplayerScreen.initAnimation();
        }
	}
	
	@Override
	public void show()
	{
		Gdx.input.setInputProcessor(null);
	    this.progress = 0f;
	}

    @Override
    public void render(float delta)
    {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        progress = MathUtils.lerp(progress, game.mapGenerator.assets.manager.getProgress(), 0.1f);
        
        if(GameConfig.MULTIPLAYER)
    	{
        	if (game.multiplayerMapGenerator.assets.manager.update() && progress >= game.multiplayerMapGenerator.assets.manager.getProgress() - 0.001f)
    		{
        		game.countdown.active = true;
        		game.setScreen(multiplayerScreen);
    		}
    	}
        else
        {
        	if (game.mapGenerator.assets.manager.update() && progress >= game.mapGenerator.assets.manager.getProgress() - 0.001f) 
        		game.setScreen(new GameScreen(game));
        }
        
        loadingBar = new Texture(Gdx.files.internal("loading_bar/bate_"+(int)(progress*11)+".png"));
        spriteBatch.begin();
        
        if(GameConfig.MULTIPLAYER)
        	spriteBatch.draw(backgroundMulti,0,0);
        else
        	spriteBatch.draw(backgroundSingle,0,0);
        
        spriteBatch.draw(loadingBar, GameConfig.Screen_Width*250/GameConfig.width, 0);
        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height)
    {
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

    @Override
    public void dispose() { }
}