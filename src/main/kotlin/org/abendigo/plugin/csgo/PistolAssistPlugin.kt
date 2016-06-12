package org.abendigo.plugin.csgo

import org.abendigo.csgo.*
import org.abendigo.csgo.Engine.clientState
import org.abendigo.csgo.offsets.m_dwIndex
import org.abendigo.util.random
import org.abendigo.util.randomFloat
import java.lang.System.currentTimeMillis

object PistolAssistPlugin : InGamePlugin("Pistol Assist", duration = 8) {

	override val author = "Jire"
	override val description = "Assists your aim with pistols"

	private const val SMOOTHING_MIN = 3.5F
	private const val SMOOTHING_MAX = 7F

	private const val MIN_ELAPSED = 75
	private const val MAX_ELAPSED = 300

	private val TARGET_BONES = arrayOf(5, 5, 6)
	private const val CHANGE_BONE_CHANCE = 18

	private const val RESET_TARGET_CHANCE = 13

	private var target: Player? = null
	private var targetBone = 5

	private val aim = Vector(0F, 0F, 0F)

	private var lastAim = 0L

	override fun cycle() {
		val elapsedTime = currentTimeMillis() - lastAim
		if (elapsedTime < random(MIN_ELAPSED, MAX_ELAPSED)) return

		if (target == null) {
			val targetAddress = +Me.targetAddress
			if (targetAddress <= 0) return

			val targetIndex = csgo.get<Int>(targetAddress + m_dwIndex) - 1
			try {
				target = Client.enemies[targetIndex]!!
			} catch (t: Throwable) {
				return
			}
		}

		if (+Me().dead || +target!!.dead || !+target!!.spotted) {
			target = null
			return
		}

		if (random(CHANGE_BONE_CHANCE) == 0) targetBone = newTargetBone()

		val enemyPosition = target!!.bonePosition(targetBone)
		val myPosition = +Me().position

		compensateVelocity(Me(), target!!, enemyPosition, randomFloat(SMOOTHING_MIN, SMOOTHING_MAX))

		calculateAngle(Me(), myPosition, enemyPosition, aim.reset())
		normalizeAngle(aim)

		val angle = clientState(1024).angle()
		normalizeAngle(angle)

		angleSmooth(aim, angle, randomFloat(SMOOTHING_MIN, SMOOTHING_MAX))

		if (random(RESET_TARGET_CHANCE) == 0) {
			target = null
			lastAim = currentTimeMillis()
		}
	}

	private fun newTargetBone() = TARGET_BONES[random(TARGET_BONES.size)]

}