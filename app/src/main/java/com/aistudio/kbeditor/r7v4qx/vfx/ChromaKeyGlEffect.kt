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

@UnstableApi
class ChromaKeyGlEffect(
    private val keyHue: String,
    private val tolerance: Float,
    private val spillSuppression: Float,
    private val backdropColorHex: Long
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return ChromaKeyShaderProgram(
            useHdr = useHdr,
            keyHue = keyHue,
            tolerance = tolerance,
            spillSuppression = spillSuppression,
            backdropColorHex = backdropColorHex
        )
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

@UnstableApi
private class ChromaKeyShaderProgram(
    useHdr: Boolean,
    keyHue: String,
    tolerance: Float,
    spillSuppression: Float,
    backdropColorHex: Long
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

        val keyRgb = when (keyHue.lowercase()) {
            "blue" -> floatArrayOf(0.05f, 0.22f, 0.92f)
            "cyan" -> floatArrayOf(0.05f, 0.85f, 0.92f)
            else -> floatArrayOf(0.08f, 0.88f, 0.16f)
        }
        val bgR = ((backdropColorHex shr 16) and 0xFF) / 255f
        val bgG = ((backdropColorHex shr 8) and 0xFF) / 255f
        val bgB = (backdropColorHex and 0xFF) / 255f

        glProgram.setFloatsUniform("uKeyColor", keyRgb)
        glProgram.setFloatsUniform("uBackdropColor", floatArrayOf(bgR, bgG, bgB))
        glProgram.setFloatUniform("uTolerance", tolerance.coerceIn(0.05f, 0.75f))
        glProgram.setFloatUniform("uSpill", spillSuppression.coerceIn(0f, 1f))
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
            uniform vec3 uKeyColor;
            uniform vec3 uBackdropColor;
            uniform float uTolerance;
            uniform float uSpill;
            varying vec2 vTexSamplingCoord;
            void main() {
              vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
              float dist = distance(src.rgb, uKeyColor);
              float mask = smoothstep(uTolerance * 0.55, uTolerance * 1.15 + 0.05, dist);
              float luma = dot(src.rgb, vec3(0.299, 0.587, 0.114));
              vec3 despill = mix(src.rgb, vec3(luma), (1.0 - mask) * uSpill * 0.5);
              vec3 finalRgb = mix(uBackdropColor, despill, mask);
              gl_FragColor = vec4(finalRgb, src.a);
            }
        """
    }
}
