package editor;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class Editor extends JFrame
{
	private PreviewPanel panel;
	private toolsPanel tools;
	private int numLevels;
	
	public Editor()
	{
		this.panel = new PreviewPanel(numLevels);
		this.tools = new toolsPanel(panel, this);
		
		this.setTitle("editor");
		Container contentPane = this.getContentPane();
		panel.setPreferredSize(new Dimension(600,600));
		tools.setPreferredSize(new Dimension(250,600));
		
		contentPane.setLayout(new BorderLayout());
		contentPane.add(panel, BorderLayout.WEST);
		contentPane.add(tools, BorderLayout.EAST);
				
		this.setJMenuBar(new Menu(panel));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(true);
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(false);
	}
	
	public void setNumLevels (int number) 	{ numLevels = number;}
	
}
