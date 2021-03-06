/**
  * Copyright (c) Lambda Innovation, 2013-2016
  * This file is part of the AcademyCraft mod.
  * https://github.com/LambdaInnovation/AcademyCraft
  * Licensed under GPLv3, see project root for more information.
  */
package cn.academy.vanilla.electromaster.skill

import cn.academy.ability.api.Skill
import cn.academy.ability.api.context.{RegClientContext, ClientContext, Context, ClientRuntime}
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc.ArcType
import cn.academy.vanilla.generic.entity.EntityRippleMark
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.util.entityx.EntityCallback
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.helper.{TickScheduler, Motion3D}
import cn.lambdalib.util.mc.{EntitySelectors, Raytrace}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.entity.player.EntityPlayer

/**
  * @author WeAthFolD, KSkun
  */
object ThunderClap extends Skill("thunder_clap", 5) {

  final val MIN_TICKS = 40
  final val MAX_TICKS = 60

  def getDamage(exp: Float, ticks: Int) = lerpf(36, 72, exp) * lerpf(1.0f, 1.2f, (ticks - 40.0f) / 60.0f)
  def getRange(exp: Float) = lerpf(15, 30, exp)
  def getCooldown(exp: Float, ticks: Int) = (ticks * lerpf(10, 6, exp)).asInstanceOf[Int]

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyid: Int) = activateSingleKey(rt, keyid, p => new ThunderClapContext(p))

}

object ThunderClapContext {

  final val MSG_START = "start"
  final val MSG_END = "end"
  final val MSG_EFFECT_START = "effect_start"
  final val MSG_EFFECT_END = "effect_end"

}

import cn.academy.ability.api.AbilityAPIExt._
import ThunderClap._
import ThunderClapContext._

class ThunderClapContext(p: EntityPlayer) extends Context(p, ThunderClap) {

  val exp = ctx.getSkillExp
  var ticks = 0
  var hitX, hitY, hitZ = 0d

  @Listener(channel=MSG_KEYDOWN, side=Array(Side.CLIENT))
  private def c_onKeyDown() = {
    sendToServer(MSG_START)
  }

  @Listener(channel=MSG_START, side=Array(Side.SERVER))
  private def s_onStart() = {
    sendToClient(MSG_EFFECT_START)

    val overload = lerpf(390, 252, exp)
    ctx.consume(overload, 0)
  }

  @Listener(channel=MSG_TICK, side=Array(Side.SERVER))
  private def s_onTick() = {
    val DISTANCE = 40.0
    val pos = Raytrace.traceLiving(player, 40.0, EntitySelectors.nothing())
    if(pos != null) {
      hitX = pos.hitVec.xCoord
      hitY = pos.hitVec.yCoord
      hitZ = pos.hitVec.zCoord
    } else {
      val mo = new Motion3D(player, true).move(DISTANCE)
      hitX = mo.px
      hitY = mo.py
      hitZ = mo.pz
    }

    ticks += 1

    val consumption = lerpf(100, 120, exp)
    if(ticks <= MIN_TICKS && !ctx.consume(0, consumption))
      sendToSelf(MSG_END)
    if(ticks >= MAX_TICKS) {
      sendToSelf(MSG_END)
    }
  }

  @Listener(channel=MSG_END, side=Array(Side.SERVER))
  private def s_onEnd(): Unit = {
    if(ticks < MIN_TICKS) {
      sendToClient(MSG_EFFECT_END)
      terminate()
      return
    }

    sendToClient(MSG_EFFECT_END)

    val lightning = new EntityLightningBolt(player.worldObj, hitX, hitY, hitZ)
    player.worldObj.addWeatherEffect(lightning)
    ctx.attackRange(hitX, hitY, hitZ, ThunderClap.getRange(exp), getDamage(exp, ticks), EntitySelectors.exclude(player))

    ctx.setCooldown(getCooldown(exp, ticks))
    ctx.addSkillExp(0.003f)
    ThunderClap.triggerAchievement(player)
    terminate()
  }

  @Listener(channel=MSG_KEYUP, side=Array(Side.CLIENT))
  private def c_onEnd() = {
    sendToServer(MSG_END)
  }

  @Listener(channel=MSG_KEYABORT, side=Array(Side.CLIENT))
  private def c_onAbort() = {
    sendToServer(MSG_END)
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegClientContext(classOf[ThunderClapContext])
class ThunderClapContextC(par: ThunderClapContext) extends ClientContext(par) {

  var surroundArc: EntitySurroundArc = null
  var mark: EntityRippleMark = null
  var ticks = 0
  var hitX, hitY, hitZ = 0d
  var canTicking = false

  @Listener(channel=MSG_EFFECT_START, side=Array(Side.CLIENT))
  private def c_spawnEffect() = {
    canTicking = true
    surroundArc = new EntitySurroundArc(player).setArcType(ArcType.BOLD)
    player.worldObj.spawnEntityInWorld(surroundArc)

    if(isLocal) {
      mark = new EntityRippleMark(player.worldObj)

      player.worldObj.spawnEntityInWorld(mark)
      mark.color.setColor4d(0.8, 0.8, 0.8, 0.7)
      mark.setPosition(hitX, hitY, hitZ)
    }
  }

  @Listener(channel=MSG_TICK, side=Array(Side.CLIENT))
  private def c_updateEffect() = {
    if(canTicking) {
      val DISTANCE = 40.0
      val pos = Raytrace.traceLiving(player, 40.0, EntitySelectors.nothing())
      if (pos != null) {
        hitX = pos.hitVec.xCoord
        hitY = pos.hitVec.yCoord
        hitZ = pos.hitVec.zCoord
      } else {
        val mo = new Motion3D(player, true).move(DISTANCE)
        hitX = mo.px
        hitY = mo.py
        hitZ = mo.pz
      }

      ticks += 1
      if (isLocal) {
        val max = 0.1f
        val min = 0.001f
        player.capabilities.setPlayerWalkSpeed(Math.max(min, max - (max - min) / 60 * ticks))
        if (mark != null) mark.setPosition(hitX, hitY, hitZ)
      }
    }
  }

  @Listener(channel=MSG_EFFECT_END, side=Array(Side.CLIENT))
  private def c_endEffect() = {
    canTicking = false
    player.capabilities.setPlayerWalkSpeed(0.1f)
    if(surroundArc != null)
      surroundArc.executeAfter(new EntityCallback[Entity] {
        override def execute(target: Entity) {
          target.setDead()
        }
      }, 10)

    if(isLocal && mark != null) {
      mark.setDead()
    }
  }

}