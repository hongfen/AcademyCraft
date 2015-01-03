/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.Set;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.TextUtils;
import cn.liutils.api.client.TrueTypeFont;
import cn.liutils.api.client.gui.widget.TextButton;
import cn.liutils.api.client.util.HudUtils;
import cn.liutils.api.client.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class PageLearn extends DevSubpage {

	public PageLearn(GuiDeveloper parent) {
		super(parent, ACLangs.pgLearn(), ACClientProps.TEX_GUI_AD_LEARNING);
		TextButton btn = new TextButton("btn_learn", this, 34F, 26F, 61.5F, 13.5F);
		btn.setTexMapping(1, 448, 123, 27);
		btn.setDownMapping(1, 419);
		btn.setTexture(ACClientProps.TEX_GUI_AD_LEARNING, 512, 512);
		btn.setTextProps(ACLangs.learnAbility(), 8);
	}

	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		GL11.glPushMatrix(); {
			//Energy bar
			double prog = dev.dev.curEnergy / dev.dev.getMaxEnergy();
			HudUtils.drawTexturedModalRect(8.5, 112.5, 17, 293, 122 * prog, 5.5, 244 * prog, 11);
			
			RenderUtils.bindColor(dev.DEFAULT_COLOR);
			//Machine stat
			String str = ACLangs.machineStat();
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 100.5, 9);
			//Current Energy
			str = String.format("%s: %.0f/%.0f EU", ACLangs.curEnergy(), dev.dev.curEnergy, dev.dev.getMaxEnergy());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 121, 8);
			//Sync Rate
			str = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.syncRateDisplay());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 129, 8);
			GL11.glColor4f(1, 1, 1, 1);
		} GL11.glPopMatrix();
	}
	
	static final int[] BUTTON_COLOR = {120, 206, 255};

}
