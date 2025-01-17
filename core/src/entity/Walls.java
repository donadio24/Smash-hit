package entity;

import com.badlogic.gdx.math.Vector3;
import videogame.GameConfig;

public enum Walls
{
    RIGHT_WALL(90,22,2), LEFT_WALL(90,22,2), BACK_WALL(2,22,90),
	CEILING(GameConfig.ROOM_ROW * GameConfig.CELL_HEIGHT,1,GameConfig.ROOM_COLUMN * GameConfig.CELL_WIDTH),
	FLOOR(GameConfig.ROOM_ROW * GameConfig.CELL_HEIGHT,1,GameConfig.ROOM_COLUMN * GameConfig.CELL_WIDTH),
	FOREWARD_UPPER_WALL(1,7,90), FOREWARD_LEFT_WALL(1,15,36), FOREWARD_RIGHT_WALL(1,15,36);
	
	Vector3 size;
	
	private Walls(float x, float y, float z)
	{
		size = new Vector3(x,y,z);
	}
}