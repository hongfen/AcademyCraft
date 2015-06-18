/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.render;

import java.util.Random;

import net.minecraft.util.Vec3;
import cn.academy.core.client.IPointFactory;
import cn.liutils.util.generic.VecUtils;

/**
 * Randomly gens a point on face of a cube with size(w, h, l). (placed at 0, 0, 0)
 * It is guaranteed that every point has the same appearing probablity.
 * @author WeathFolD
 */
public class CubePointFactory implements IPointFactory {
	
	/*
	 * Face id: 
	 * 0 -Y
	 * 1 +Y
	 * 2 -Z
	 * 3 +Z
	 * 4 -X
	 * 5 +X
	 */
	private double w, h, l;
	private static final Random RNG = new Random();
	boolean centered;

	public CubePointFactory(double _w, double _h, double _l) {
		setSize(_w, _h, _l);
	}
	
	public CubePointFactory setCentered(boolean b) {
		centered = b;
		return this;
	}
	
	public void setSize(double _w, double _h, double _l) {
		w = _w;
		h = _h;
		l = _l;
	}
	
	private double getArea(int f) {
		if(f == 0 || f == 1) {
			return w * l;
		}
		if(f == 2 || f == 3) {
			return h * l;
		}
		return h * w;
	}
	
	private int randFace() {
		return RNG.nextInt(6);
	}

	@Override
	public Vec3 next() {
		int face = randFace();
		double a, b;
		double xOffset = 0, zOffset = 0;
		if(centered) {
			xOffset = -w * 0.5;
			zOffset = -l * 0.5;
		}
		switch(face) {
		case 0:
		case 1:
			a = RNG.nextDouble() * w;
			b = RNG.nextDouble() * l;
			return VecUtils.vec(a + xOffset, face == 0 ? 0 : h, b + zOffset);
		case 2:
		case 3:
			a = RNG.nextDouble() * h;
			b = RNG.nextDouble() * w;
			return VecUtils.vec(b + xOffset, a, (face == 2 ? 0 : l) + zOffset);
		case 4:
		case 5:
			a = RNG.nextDouble() * h;
			b = RNG.nextDouble() * l;
			return VecUtils.vec((face == 4 ? 0 : w) + xOffset, a, b + zOffset);
		}
		return null; //Not supposed to happen
	}

}