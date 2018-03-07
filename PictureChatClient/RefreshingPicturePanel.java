import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;


public class RefreshingPicturePanel extends JPanel
{
private Image image;
	  
public RefreshingPicturePanel(Image imageToShow)
  {
  image = imageToShow;  
  }
	  
@Override
public void paint(Graphics g)
  {
  g.drawImage(image,0,0,this);
  }

}