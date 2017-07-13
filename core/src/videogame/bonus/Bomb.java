package videogame.bonus;

import com.badlogic.gdx.math.Vector3;

import entity.AbstractGameObject;
import entity.Destroyable;
import videogame.GameConfig;

public class Bomb extends AbstractGameObject 
{	
	private Vector3 velocity = new Vector3(20,1,20);
	private Vector3 speed;
	private Vector3 direction;
	private boolean active = false;
	private boolean shooted = false;
	
	public Bomb()
	{
		super(GameConfig.player.getPosition(),null);
	}

	public final boolean inAction() {return active;}
	public final boolean shooted()	{return shooted;}
	
	public void manageBomb(float delta)
	{
		if(!shooted())
			setPosition(GameConfig.player.getPosition().cpy().add(0,6.5f,-2));
		else
		{
			update(delta);
		
			int	x = (int) ((getX() + 4.5f) / GameConfig.CELL_HEIGHT) % GameConfig.ROOM_ROW;
	    	int	y = (int) ((getZ() + 3.5f) / GameConfig.CELL_WIDTH) % GameConfig.ROOM_COLUMN;
	    
	    	synchronized(GameConfig.tools)
	    	{
	    		Destroyable object = GameConfig.tools.get(GameConfig.actualLevel-1)[x][y];
	    		if(object != null)
	    		{
	    			if(collide(object) && getY() <= object.getY() + object.getSize().y/2)
	    				active = false;
	    		}
	    	}
	    	
	    	if(getY() < -5)
	    		active = false;
		}
	}
	
	public void update(float delta)
	{
		Position.x += speed.x * delta;
		Position.y += speed.y * delta  - (GameConfig.GRAVITY*delta*delta);
		Position.z += speed.z * delta;
				
		speed.y -= GameConfig.GRAVITY * delta;
	}

	public void shoot()
	{
		active = true;
		shooted = true;
		
		direction = GameConfig.DIRECTION;
		speed = new Vector3(velocity.x*direction.x, velocity.y*direction.y, velocity.z*direction.z);
	}
}