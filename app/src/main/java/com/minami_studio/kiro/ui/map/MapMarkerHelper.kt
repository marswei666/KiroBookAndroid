package com.minami_studio.kiro.ui.map

import android.graphics.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.PathParser
import com.minami_studio.kiro.ui.theme.WanderAccent
import com.minami_studio.kiro.ui.theme.WanderInk

fun createCategoryMarkerBitmap(icon: ImageVector): Bitmap {
    val iconTint = WanderInk
    val iconTintInt = Color.rgb(
        (iconTint.red * 255).toInt(),
        (iconTint.green * 255).toInt(),
        (iconTint.blue * 255).toInt()
    )
    val scale = 3f
    val capsuleW = 30 * scale
    val capsuleH = 30 * scale
    val dotR = 3 * scale
    val shadowPad = 6 * scale
    val totalW = (capsuleW + shadowPad * 2).toInt()
    val totalH = (capsuleH + dotR * 2 + shadowPad * 2 + 4 * scale).toInt()
    val bmp = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)

    val cx = totalW / 2f
    val capsuleTop = shadowPad
    val rect = RectF(
        cx - capsuleW / 2, capsuleTop,
        cx + capsuleW / 2, capsuleTop + capsuleH
    )

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26000000
        maskFilter = BlurMaskFilter(4 * scale, BlurMaskFilter.Blur.NORMAL)
    }
    c.drawRoundRect(
        RectF(rect.left, rect.top + 2 * scale, rect.right, rect.bottom + 2 * scale),
        capsuleH / 2, capsuleH / 2, shadowPaint
    )

    val bgColor = WanderAccent
    val bgColorInt = Color.rgb(
        (bgColor.red * 255).toInt(),
        (bgColor.green * 255).toInt(),
        (bgColor.blue * 255).toInt()
    )
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColorInt }
    c.drawRoundRect(rect, capsuleH / 2, capsuleH / 2, bgPaint)

    val iconScale = scale * 0.65f
    val iconW = icon.viewportWidth
    val iconH = icon.viewportHeight
    val offsetX = cx - (iconW * iconScale) / 2
    val offsetY = capsuleTop + (capsuleH - iconH * iconScale) / 2

    c.save()
    c.translate(offsetX, offsetY)
    c.scale(iconScale, iconScale)

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    renderVectorNode(c, icon.root, iconPaint, iconTintInt)
    c.restore()

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColorInt }
    c.drawCircle(cx, capsuleTop + capsuleH + dotR + 2 * scale, dotR, dotPaint)

    return bmp
}

@Composable
fun rememberCategoryMarkerBitmap(icon: ImageVector): Bitmap {
    return remember(icon) { createCategoryMarkerBitmap(icon) }
}

@Composable
fun rememberGoogleCategoryMarker(icon: ImageVector): com.google.android.gms.maps.model.BitmapDescriptor {
    val bitmap = rememberCategoryMarkerBitmap(icon)
    return remember(icon) {
        com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

@Composable
fun rememberAmapCategoryMarker(icon: ImageVector): com.amap.api.maps.model.BitmapDescriptor {
    val bitmap = rememberCategoryMarkerBitmap(icon)
    return remember(icon) {
        com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

private fun pathDataToSvg(data: List<androidx.compose.ui.graphics.vector.PathNode>): String {
    val sb = StringBuilder()
    for (node in data) {
        when (node) {
            is androidx.compose.ui.graphics.vector.PathNode.MoveTo ->
                sb.append("M${node.x},${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeMoveTo ->
                sb.append("m${node.dx},${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.LineTo ->
                sb.append("L${node.x},${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeLineTo ->
                sb.append("l${node.dx},${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.HorizontalTo ->
                sb.append("H${node.x}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeHorizontalTo ->
                sb.append("h${node.dx}")
            is androidx.compose.ui.graphics.vector.PathNode.VerticalTo ->
                sb.append("V${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeVerticalTo ->
                sb.append("v${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.CurveTo ->
                sb.append("C${node.x1},${node.y1},${node.x2},${node.y2},${node.x3},${node.y3}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeCurveTo ->
                sb.append("c${node.dx1},${node.dy1},${node.dx2},${node.dy2},${node.dx3},${node.dy3}")
            is androidx.compose.ui.graphics.vector.PathNode.ReflectiveCurveTo ->
                sb.append("S${node.x1},${node.y1},${node.x2},${node.y2}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeReflectiveCurveTo ->
                sb.append("s${node.dx1},${node.dy1},${node.dx2},${node.dy2}")
            is androidx.compose.ui.graphics.vector.PathNode.QuadTo ->
                sb.append("Q${node.x1},${node.y1},${node.x2},${node.y2}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeQuadTo ->
                sb.append("q${node.dx1},${node.dy1},${node.dx2},${node.dy2}")
            is androidx.compose.ui.graphics.vector.PathNode.ReflectiveQuadTo ->
                sb.append("T${node.x},${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeReflectiveQuadTo ->
                sb.append("t${node.dx},${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.ArcTo ->
                sb.append("A${node.horizontalEllipseRadius},${node.verticalEllipseRadius},${node.theta},${if (node.isMoreThanHalf) 1 else 0},${if (node.isPositiveArc) 1 else 0},${node.arcStartX},${node.arcStartY}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeArcTo ->
                sb.append("a${node.horizontalEllipseRadius},${node.verticalEllipseRadius},${node.theta},${if (node.isMoreThanHalf) 1 else 0},${if (node.isPositiveArc) 1 else 0},${node.arcStartDx},${node.arcStartDy}")
            is androidx.compose.ui.graphics.vector.PathNode.Close ->
                sb.append("Z")
        }
    }
    return sb.toString()
}

private fun renderVectorNode(
    c: Canvas,
    node: androidx.compose.ui.graphics.vector.VectorNode,
    paint: Paint,
    tintColor: Int
) {
    when (node) {
        is androidx.compose.ui.graphics.vector.VectorPath -> {
            val svgPath = pathDataToSvg(node.pathData)
            if (svgPath.isNotEmpty()) {
                PathParser.createPathFromPathData(svgPath)?.let { androidPath ->
                    paint.color = tintColor
                    if (node.fill != null) {
                        paint.style = Paint.Style.FILL
                        c.drawPath(androidPath, paint)
                    }
                    if (node.stroke != null) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = node.strokeLineWidth
                        c.drawPath(androidPath, paint)
                    }
                }
            }
        }
        is androidx.compose.ui.graphics.vector.VectorGroup -> {
            c.save()
            if (node.translationX != 0f) c.translate(node.translationX, 0f)
            if (node.translationY != 0f) c.translate(0f, node.translationY)
            if (node.scaleX != 1f) c.scale(node.scaleX, 1f)
            if (node.scaleY != 1f) c.scale(1f, node.scaleY)
            for (child in node) {
                renderVectorNode(c, child, paint, tintColor)
            }
            c.restore()
        }
    }
}
