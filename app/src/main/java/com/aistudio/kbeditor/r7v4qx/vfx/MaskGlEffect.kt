package com.aistudio.kbeditor.r7v4qx.vfx

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.aistudio.kbeditor.r7v4qx.model.MaskType

@UnstableApi
class MaskGlEffect(
    private val maskType: MaskType,
    private val rotationDeg: Float = 0f,
    private val inverted: Boolean = false,
    private val maskScale: Float = 0.85f,
    private val maskOffsetX: Float = 0f,
    private val maskOffsetY: Float = 0f,
    private val maskScaleX: Float = 1.0f,
    private val maskScaleY: Float = 1.0f,
    private val maskFeather: Float = 0.08f
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return MaskShaderProgram(useHdr, maskType, rotationDeg, inverted, maskScale, maskOffsetX, maskOffsetY, maskScaleX, maskScaleY, maskFeather)
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return maskType == MaskType.NONE
    }
}

@UnstableApi
private class MaskShaderProgram(
    useHdr: Boolean,
    maskType: MaskType,
    rotationDeg: Float,
    inverted: Boolean,
    maskScale: Float,
    maskOffsetX: Float,
    maskOffsetY: Float,
    maskScaleX: Float,
    maskScaleY: Float,
    maskFeather: Float
) : BaseGlShaderProgram(useHdr, 1) {

    private val glProgram: GlProgram

    init {
        try {
            glProgram = GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }

        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        val identity = GlUtil.create4x4IdentityMatrix()
        glProgram.setFloatsUniform("uTransformationMatrix", identity)
        glProgram.setFloatsUniform("uTexTransformationMatrix", identity)

        val mode = when (maskType) {
            MaskType.CIRCLE -> 1.0f
            MaskType.ROUNDED_RECT -> 2.0f
            MaskType.SPLIT_WIPE -> 3.0f
            MaskType.ANAMORPHIC_BARS -> 4.0f
            MaskType.MIRROR -> 5.0f
            MaskType.HEART -> 6.0f
            MaskType.STAR -> 7.0f
            MaskType.NONE -> 0.0f
        }
        glProgram.setFloatUniform("uMaskMode", mode)
        glProgram.setFloatUniform("uMaskAngle", Math.toRadians(rotationDeg.toDouble()).toFloat())
        glProgram.setFloatUniform("uInvert", if (inverted) 1.0f else 0.0f)
        glProgram.setFloatUniform("uMaskScale", maskScale.coerceIn(0.25f, 1.4f))
        glProgram.setFloatUniform("uMaskOffsetX", maskOffsetX.coerceIn(-0.45f, 0.45f))
        glProgram.setFloatUniform("uMaskOffsetY", maskOffsetY.coerceIn(-0.45f, 0.45f))
        glProgram.setFloatUniform("uMaskScaleX", maskScaleX.coerceIn(0.25f, 2.5f))
        glProgram.setFloatUniform("uMaskScaleY", maskScaleY.coerceIn(0.25f, 2.5f))
        glProgram.setFloatUniform("uMaskFeather", maskFeather.coerceIn(0.004f, 0.45f))
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aFramePosition;
            uniform mat4 uTransformationMatrix;
            uniform mat4 uTexTransformationMatrix;
            varying vec2 vTexSamplingCoord;
            void main() {
              gl_Position = uTransformationMatrix * aFramePosition;
              vec4 texCoord = vec4(aFramePosition.x * 0.5 + 0.5, aFramePosition.y * 0.5 + 0.5, 0.0, 1.0);
              vTexSamplingCoord = (uTexTransformationMatrix * texCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform float uMaskMode;
            uniform float uMaskAngle;
            uniform float uInvert;
            uniform float uMaskScale;
            uniform float uMaskOffsetX;
            uniform float uMaskOffsetY;
            uniform float uMaskScaleX;
            uniform float uMaskScaleY;
            uniform float uMaskFeather;
            varying vec2 vTexSamplingCoord;
            void main() {
              vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
              vec2 c = (vTexSamplingCoord - vec2(0.5 + uMaskOffsetX, 0.5 - uMaskOffsetY)) / max(uMaskScale * vec2(uMaskScaleX, uMaskScaleY), vec2(0.08));
              float ca = cos(uMaskAngle);
              float sa = sin(uMaskAngle);
              vec2 uv = vec2(c.x * ca - c.y * sa, c.x * sa + c.y * ca) + vec2(0.5, 0.5);
              float inside = 1.0;
              if (uMaskMode > 0.5 && uMaskMode < 1.5) {
                float d = distance(uv, vec2(0.5, 0.5));
                inside = 1.0 - smoothstep(0.48 - uMaskFeather, 0.48 + uMaskFeather, d);
              } else if (uMaskMode >= 1.5 && uMaskMode < 2.5) {
                vec2 q = abs(uv - vec2(0.5)) - vec2(0.40);
                float roundedDist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - 0.08;
                inside = 1.0 - smoothstep(-uMaskFeather, uMaskFeather, roundedDist);
              } else if (uMaskMode >= 2.5 && uMaskMode < 3.5) {
                inside = step(uv.x, 0.55);
              } else if (uMaskMode >= 3.5 && uMaskMode < 4.5) {
                inside = step(0.12, uv.y) * step(uv.y, 0.88);
              } else if (uMaskMode >= 4.5 && uMaskMode < 5.5) {
                inside = step(0.22, uv.y) * step(uv.y, 0.78);
              } else if (uMaskMode >= 5.5 && uMaskMode < 6.5) {
                vec2 p = (uv - vec2(0.5, 0.48)) * vec2(2.3, -2.3);
                p.y -= sqrt(abs(p.x)) * 0.45;
                inside = step(dot(p, p), 0.75);
              } else if (uMaskMode >= 6.5) {
                vec2 p = (uv - vec2(0.5, 0.5)) * 2.2;
                float a = atan(p.y, p.x);
                float r = length(p);
                float starR = 0.62 + 0.30 * cos(5.0 * a);
                inside = step(r, starR);
              }
              float finalMask = mix(inside, 1.0 - inside, uInvert);
              gl_FragColor = vec4(src.rgb * finalMask, src.a);
            }
        """
    }
}
