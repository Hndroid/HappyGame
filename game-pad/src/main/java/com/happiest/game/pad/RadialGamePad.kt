/*
 * Created by Filippo Scognamiglio.
 * Copyright (c) 2020. This file is part of RadialGamePad.
 *
 * RadialGamePad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RadialGamePad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RadialGamePad.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("unused")

package com.happiest.game.pad

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.*
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.jakewharton.rxrelay2.PublishRelay
import com.happiest.game.pad.accessibility.AccessibilityBox
import com.happiest.game.pad.config.PrimaryDialConfig
import com.happiest.game.pad.config.RadialGamePadConfig
import com.happiest.game.pad.config.SecondaryDialConfig
import com.happiest.game.pad.dials.*
import com.happiest.game.pad.event.Event
import com.happiest.game.pad.event.EventsSource
import com.happiest.game.pad.event.GestureType
import com.happiest.game.pad.haptics.*
import com.happiest.game.pad.math.MathUtils.clamp
import com.happiest.game.pad.math.MathUtils.toRadians
import com.happiest.game.pad.math.Sector
import com.happiest.game.pad.simulation.SimulateKeyDial
import com.happiest.game.pad.simulation.SimulateMotionDial
import com.happiest.game.pad.touchbound.CircleTouchBound
import com.happiest.game.pad.touchbound.SectorTouchBound
import com.happiest.game.pad.utils.Constants
import com.happiest.game.pad.utils.MultiTapDetector
import com.happiest.game.pad.utils.PaintUtils
import com.happiest.game.pad.utils.PaintUtils.scale
import com.happiest.game.pad.utils.TouchUtils
import io.reactivex.Observable
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan
import kotlin.properties.Delegates

class RadialGamePad @JvmOverloads constructor(
    private val gamePadConfig: RadialGamePadConfig,
    defaultMarginsInDp: Float = 16f,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), EventsSource {

    private val eventsSubject = PublishRelay.create<Event>()

    private val exploreByTouchHelper = object : ExploreByTouchHelper(this) {

        private fun computeVirtualViews(): Map<Int, AccessibilityBox> {
            return allInteractors
                .flatMap { it.dial.accessibilityBoxes() }
                .sortedBy { it.rect.top }
                .mapIndexed { index, accessibilityBox -> index to accessibilityBox }
                .toMap()
        }

        override fun getVirtualViewAt(x: Float, y: Float): Int {
            return computeVirtualViews().entries
                .filter { (_, accessibilityBox) -> accessibilityBox.rect.contains(x.roundToInt(), y.roundToInt()) }
                .map { (id, _) -> id }
                .firstOrNull() ?: INVALID_ID
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            computeVirtualViews().forEach { (id, _) ->  virtualViewIds.add(id) }
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            return false
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat
        ) {
            val virtualView = computeVirtualViews()[virtualViewId]
            node.setBoundsInParent(virtualView!!.rect)
            node.contentDescription = virtualView.text
        }
    }

    private val marginsInPixel: Int = PaintUtils.convertDpToPixel(defaultMarginsInDp, context).roundToInt()

    private var dials: Int = gamePadConfig.sockets
    private var size: Float = 0f
    private var center = PointF(0f, 0f)
    private var positionOnScreen = intArrayOf(0, 0)

    /** Change the horizontal gravity of the gamepad. Use in range [-1, +1] you can move the pad
     *  left or right. This value is not considered when sizing, so the actual shift depends on the
     *  view size.*/
    var gravityX: Float by Delegates.observable(0f) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Change the vertical gravity of the gamepad. Use in range [-1, +1] you can move the pad
     *  up or down. This value is not considered when sizing, so the actual shift depends on the
     *  view size.*/
    var gravityY: Float by Delegates.observable(0f) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Shift the gamepad left or right by this size in pixel. This value is not considered when
     *  sizing and the shift only happens if there is room for it. It is capped so that the
     *  pad is never cropped.*/
    var offsetX: Float by Delegates.observable(0f) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Shift the gamepad top or bottom by this size in pixel. This value is not considered when
     *  sizing and the shift only happens if there is room for it. It is capped so that the
     *  pad is never cropped.*/
    var offsetY: Float by Delegates.observable(0f) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Limit the size of the actual gamepad inside the view.*/
    var primaryDialMaxSizeDp: Float = Float.MAX_VALUE
        set(value) {
            field = value
            requestLayoutAndInvalidate()
        }

    /** Rotate the secondary dials by this value in degrees.*/
    var secondaryDialRotation: Float by Delegates.observable(0f) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Increase the spacing between primary and secondary dials. Use in range [0, 1].*/
    var secondaryDialSpacing: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            requestLayoutAndInvalidate()
        }

    /** Add spacing at the top. This space will not be considered when drawing and
     *  sizing the gamepad. Touch events in the area will still be forwarded to the View.*/
    var spacingTop: Int by Delegates.observable(0) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Add spacing at the bottom. This space will not be considered when drawing and
     *  sizing the gamepad. Touch events in the area will still be forwarded to the View.*/
    var spacingBottom: Int by Delegates.observable(0) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Add spacing at the left. This space will not be considered when drawing and
     *  sizing the gamepad. Touch events in the area will still be forwarded to the View.*/
    var spacingLeft: Int by Delegates.observable(0) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    /** Add spacing at the right. This space will not be considered when drawing and
     *  sizing the gamepad. Touch events in the area will still be forwarded to the View.*/
    var spacingRight: Int by Delegates.observable(0) { _, _, _ ->
        requestLayoutAndInvalidate()
    }

    private val hapticEngine = createHapticEngine()

    private lateinit var primaryInteractor: DialInteractor
    private lateinit var secondaryInteractors: List<DialInteractor>
    private lateinit var allInteractors: List<DialInteractor>

    private val longPressDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val events = mutableListOf<Event>()
                val updated = allInteractors.map {
                    it.gesture(e.x, e.y, GestureType.LONG_PRESS, events)
                }

                if (updated.any { it }) {
                    postInvalidate()
                }

                handleEvents(events)
            }
        }
    )

    private val tapsDetector: MultiTapDetector = MultiTapDetector(context) { x, y, taps, isConfirmed ->
        if (!isConfirmed) return@MultiTapDetector

        val gestureType = when (taps) {
            0 -> GestureType.FIRST_TOUCH
            1 -> GestureType.SINGLE_TAP
            2 -> GestureType.DOUBLE_TAP
            3 -> GestureType.TRIPLE_TAP
            else -> null
        } ?: return@MultiTapDetector

        val events = mutableListOf<Event>()

        val updated = allInteractors.map {
            it.gesture(x, y, gestureType, events)
        }

        if (updated.any { it }) {
            postInvalidate()
        }

        handleEvents(events)
    }

    private fun createHapticEngine(): HapticEngine {
        return when (gamePadConfig.haptic) {
            HapticConfig.OFF -> NoHapticEngine()
            HapticConfig.PRESS -> SimpleHapticEngine()
            HapticConfig.PRESS_AND_RELEASE -> AdvancedHapticEngine()
        }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        primaryInteractor = buildPrimaryInteractor(gamePadConfig.primaryDial)
        secondaryInteractors = buildSecondaryInteractors(gamePadConfig.secondaryDials)
        allInteractors = listOf(primaryInteractor) + secondaryInteractors
        ViewCompat.setAccessibilityDelegate(this, exploreByTouchHelper)
    }

    /** Simulate a motion event. It's used in Lemuroid to map events from sensors. */
    fun simulateMotionEvent(id: Int, relativeX: Float, relativeY: Float) {
        val events = mutableListOf<Event>()

        val updated = allDials().filterIsInstance(SimulateMotionDial::class.java)
            .map { it.simulateMotion(id, relativeX, relativeY, events) }
            .any { it }

        if (updated) {
            postInvalidate()
        }

        handleEvents(events)
    }

    /** Programmatically clear motion events associated with the id. */
    fun simulateClearMotionEvent(id: Int) {
        val events = mutableListOf<Event>()

        val updated = allDials().filterIsInstance(SimulateMotionDial::class.java)
            .map { it.clearSimulatedMotion(id, events) }
            .any { it }

        if (updated) {
            postInvalidate()
        }

        handleEvents(events)
    }

    /** Simulate a key event. It's used in Lemuroid to map events from sensors. */
    fun simulateKeyEvent(id: Int, pressed: Boolean) {
        val events = mutableListOf<Event>()

        val updated = allDials().filterIsInstance(SimulateKeyDial::class.java)
            .map { it.simulateKeyPress(id, pressed, events) }
            .any { it }

        if (updated) {
            postInvalidate()
        }

        handleEvents(events)
    }

    /** Simulate a key event. It's used in Lemuroid to map events from sensors. */
    fun simulateClearKeyEvent(id: Int) {
        val events = mutableListOf<Event>()

        val updated = allDials().filterIsInstance(SimulateKeyDial::class.java)
            .map { it.clearSimulateKeyPress(id, events) }
            .any { it }

        if (updated) {
            postInvalidate()
        }

        handleEvents(events)
    }

    private fun handleEvents(events: List<Event>) {
        hapticEngine.performHapticForEvents(events, this)
        events.forEach { eventsSubject.accept(it) }
    }

    private fun buildPrimaryInteractor(configuration: PrimaryDialConfig): DialInteractor {
        val primaryDial = when (configuration) {
            is PrimaryDialConfig.Cross -> CrossDial(
                context,
                configuration.crossConfig,
                configuration.crossConfig.theme ?: gamePadConfig.theme
            )
            is PrimaryDialConfig.Stick -> StickDial(
                context,
                configuration.id,
                configuration.buttonPressId,
                configuration.supportsGestures,
                configuration.contentDescription,
                configuration.theme ?: gamePadConfig.theme
            )
            is PrimaryDialConfig.PrimaryButtons -> PrimaryButtonsDial(
                context,
                configuration.dials,
                configuration.center,
                toRadians(configuration.rotationInDegrees),
                configuration.allowMultiplePressesSingleFinger,
                configuration.theme ?: gamePadConfig.theme
            )
        }
        return DialInteractor(primaryDial)
    }

    private fun buildSecondaryInteractors(secondaryDials: List<SecondaryDialConfig>): List<DialInteractor> {
        return secondaryDials.map { config ->
            val secondaryDial = when (config) {
                is SecondaryDialConfig.Stick -> StickDial(
                    context,
                    config.id,
                    config.buttonPressId,
                    config.supportsGestures,
                    config.contentDescription,
                    config.theme ?: gamePadConfig.theme
                )
                is SecondaryDialConfig.SingleButton -> ButtonDial(
                    context,
                    config.spread,
                    config.buttonConfig,
                    config.theme ?: gamePadConfig.theme
                )

                is SecondaryDialConfig.Empty -> EmptyDial()
                is SecondaryDialConfig.Cross -> CrossDial(
                    context,
                    config.crossConfig,
                    config.crossConfig.theme ?: gamePadConfig.theme
                )
            }
            DialInteractor(secondaryDial)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val extendedSize = computeTotalSizeAsSizeMultipliers()

        applyMeasuredDimensions(widthMeasureSpec, heightMeasureSpec, extendedSize)

        val usableWidth = measuredWidth - spacingLeft - spacingRight - 2 * marginsInPixel
        val usableHeight = measuredHeight - spacingTop - spacingBottom - 2 * marginsInPixel

        size = minOf(
            usableWidth / extendedSize.width(),
            usableHeight / extendedSize.height(),
            PaintUtils.convertDpToPixel(primaryDialMaxSizeDp, context) / 2f
        )

        val maxDisplacementX = (usableWidth - size * extendedSize.width()) / 2f
        val maxDisplacementY = (usableHeight - size * extendedSize.height()) / 2f

        val totalDisplacementX = gravityX * maxDisplacementX + offsetX
        val finalOffsetX = clamp(totalDisplacementX, -maxDisplacementX, maxDisplacementX)

        val totalDisplacementY = gravityY * maxDisplacementY + offsetY
        val finalOffsetY = clamp(totalDisplacementY, -maxDisplacementY, maxDisplacementY)

        val baseCenterX = spacingLeft + (measuredWidth - spacingLeft - spacingRight) / 2f
        val baseCenterY = spacingTop + (measuredHeight - spacingTop - spacingBottom) / 2f

        center.x = finalOffsetX + baseCenterX - (extendedSize.left + extendedSize.right) * size * 0.5f
        center.y = finalOffsetY + baseCenterY - (extendedSize.top + extendedSize.bottom) * size * 0.5f

        measurePrimaryDial()
        measureSecondaryDials()
    }

    private fun applyMeasuredDimensions(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        extendedSize: RectF
    ) {
        val (widthMode, width) = extractModeAndDimension(widthMeasureSpec)
        val (heightMode, height) = extractModeAndDimension(heightMeasureSpec)

        val usableWidth = width - spacingLeft - spacingRight - 2 * marginsInPixel
        val usableHeight = height - spacingBottom - spacingTop - 2 * marginsInPixel

        val enforcedMaxSize = PaintUtils.convertDpToPixel(primaryDialMaxSizeDp, context) / 2

        when {
            widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST -> {
                setMeasuredDimension(
                    width,
                    minOf(
                        usableHeight,
                        (usableWidth * extendedSize.height() / extendedSize.width()).roundToInt(),
                        (enforcedMaxSize * extendedSize.height()).roundToInt()
                    ) + spacingBottom + spacingTop + 2 * marginsInPixel
                )
            }
            widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY -> {
                setMeasuredDimension(
                    minOf(
                        usableWidth,
                        (usableHeight * extendedSize.width() / extendedSize.height()).roundToInt(),
                        (enforcedMaxSize * extendedSize.width()).roundToInt()
                    ) + spacingLeft + spacingRight + 2 * marginsInPixel,
                    height
                )
            }
            else -> setMeasuredDimension(width, height)
        }
    }

    private fun extractModeAndDimension(widthMeasureSpec: Int): Pair<Int, Int> {
        return MeasureSpec.getMode(widthMeasureSpec) to MeasureSpec.getSize(widthMeasureSpec)
    }

    /** Different dial configurations cause the view to grow in different directions. This functions
     *  returns a bounding box as multipliers of 'size' that contains the whole view. They are later
     *  used to compute the actual size. */
    private fun computeTotalSizeAsSizeMultipliers(): RectF {
        val allSockets = gamePadConfig.secondaryDials

        val sizes = allSockets.map { config ->
            if (config.avoidClipping) {
                measureSecondaryDialDrawingBoxNoClipping(config)
            } else {
                measureSecondaryDialDrawingBox(config)
            }
        }

        return PaintUtils.mergeRectangles(listOf(RectF(-1f, -1f, 1f, 1f)) + sizes)
    }

    private fun measureSecondaryDials() {
        gamePadConfig.secondaryDials.forEachIndexed { index, config ->
            val (rect, sector) = measureSecondaryDial(config)
            secondaryInteractors[index].touchBound = SectorTouchBound(sector)
            secondaryInteractors[index].measure(rect, sector)
        }
    }

    private fun measurePrimaryDial() {
        primaryInteractor.measure(RectF(center.x - size, center.y - size, center.x + size, center.y + size))
        primaryInteractor.touchBound = CircleTouchBound(center, size)
    }

    private fun measureSecondaryDial(config: SecondaryDialConfig): Pair<RectF, Sector> {
        val rect = measureSecondaryDialDrawingBox(config).scale(size)
        rect.offset(center.x, center.y)

        val dialAngle = Constants.PI2 / dials
        val dialSize = DEFAULT_SECONDARY_DIAL_SCALE * size * config.scale
        val offset = size * secondaryDialSpacing
        val finalRotation = computeRotationInRadiansForDial(config)

        val sector = Sector(
            PointF(center.x, center.y),
            size + offset,
            size + offset + dialSize * config.scale,
            finalRotation + config.index * dialAngle - dialAngle / 2,
            finalRotation + (config.index + config.spread - 1) * dialAngle + dialAngle / 2
        )

        return rect to sector
    }

    private fun computeRotationInRadiansForDial(config: SecondaryDialConfig): Float {
        return toRadians(config.processSecondaryDialRotation(secondaryDialRotation))
    }

    private fun measureSecondaryDialDrawingBoxNoClipping(config: SecondaryDialConfig): RectF {
        val drawingBoxes = (config.index until (config.index + config.spread))
            .map { measureSecondaryDialDrawingBox(config, it, 1) }

        return PaintUtils.mergeRectangles(drawingBoxes)
    }

    private fun measureSecondaryDialDrawingBox(config: SecondaryDialConfig): RectF {
        return measureSecondaryDialDrawingBox(config, null, null)
    }

    private fun measureSecondaryDialDrawingBox(
        config: SecondaryDialConfig,
        overrideIndex: Int?,
        overrideSpread: Int?
    ): RectF {
        val index = overrideIndex ?: config.index
        val spread = overrideSpread ?: config.spread
        val dialAngle = Constants.PI2 / dials
        val dialSize = DEFAULT_SECONDARY_DIAL_SCALE * config.scale
        val offset = secondaryDialSpacing
        val distanceToCenter = offset + maxOf(
            0.5f * dialSize / tan(dialAngle * spread / 2f),
            1.0f + dialSize / 2f
        )

        val finalIndex = index + (spread - 1) * 0.5f
        val finalAngle = finalIndex * dialAngle + computeRotationInRadiansForDial(config)

        return RectF(
            (cos(finalAngle) * distanceToCenter - dialSize / 2f),
            (-sin(finalAngle) * distanceToCenter - dialSize / 2f),
            (cos(finalAngle) * distanceToCenter + dialSize / 2f),
            (-sin(finalAngle) * distanceToCenter + dialSize / 2f)
        )
    }

    private fun requestLayoutAndInvalidate() {
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        primaryInteractor.draw(canvas)

        secondaryInteractors.forEach {
            it.draw(canvas)
        }
    }

    override fun events(): Observable<Event> {
        return eventsSubject
    }

    fun performHapticFeedback() {
        hapticEngine.performHaptic(HapticEngine.EFFECT_PRESS, this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        tapsDetector.handleEvent(event)
        longPressDetector.onTouchEvent(event)

        val fingers = extractFingersPositions(event).toList()

        val trackedFingers = allDials()
            .map { it.trackedPointersIds() }
            .reduceRight { a, b -> a.union(b) }

        val events = mutableListOf<Event>()

        val updated = allInteractors.map { dial ->
            dial.forwardTouch(fingers, events, trackedFingers)
        }

        if (updated.any { it }) {
            postInvalidate()
        }

        handleEvents(events)

        return true
    }

    private fun extractFingersPositions(event: MotionEvent): Sequence<TouchUtils.FingerPosition> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLocationOnScreen(positionOnScreen)
            TouchUtils.extractRawFingersPositions(event, positionOnScreen[0], positionOnScreen[1])
        } else {
            TouchUtils.extractFingersPositions(event)
        }
    }

    private fun forwardTouchToDial(
        dial: DialInteractor,
        fingers: List<TouchUtils.FingerPosition>,
        trackedFingers: Set<Int>,
        outEvents: MutableList<Event>
    ): Boolean {
        return dial.forwardTouch(fingers, outEvents, trackedFingers)
    }

    private fun allDials(): List<Dial> = allInteractors.map { it.dial }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (exploreByTouchHelper.dispatchHoverEvent(event)) {
            return true
        }
        return super.dispatchHoverEvent(event)
    }

    companion object {
        const val DEFAULT_SECONDARY_DIAL_SCALE = 0.75f
    }
}
