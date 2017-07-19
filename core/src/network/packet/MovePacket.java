package network.packet;

import network.ServerThread;
import network.Server;

public class MovePacket extends Packet
{
	private float x, y, z;
	public MovePacket(float _x, float _y, float _z)
	{
		x = _x;
		y = _y;
		z = _z;
	}
		
	@Override
	public String toString() {return ("move,"+x+","+y+","+z);}
	
	
}
