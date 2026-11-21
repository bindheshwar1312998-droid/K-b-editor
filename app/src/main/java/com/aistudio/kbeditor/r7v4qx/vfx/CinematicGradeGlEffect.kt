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
class CinematicGradeGlEffect(
    private val vignette: Float,
    private val highlights: Float,
    private val shadows: Float,
    private val beautyGlow: Float,
    private val sharpen: Float = 0f
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return CinematicGradeShaderProgram(useHdr, vignette, highlights, shadows, beautyGlow, sharpen)
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return vignette <= 0.01f &&
            kotlin.math.abs(highlights) <= 0.01f &&
            kotlin.math.abs(shadows) <= 0.01f &&
            beautyGlow <= 0.01f &&
            sharpen <= 0.01f
    }
}

@UnstableApi
private class CinematicGradeShaderProgram(
    useHdr: Boolean,
    vignette: Float,
    highlights: Float,
    shadows: Float,
    beautyGlow: Float,
    sharpen: Float
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
        glProgram.setFloatUniform("uVignette", vignette.coerceIn(0f, 1f))
        glProgram.setFloatUniform("uHighlights", highlights.coerceIn(-0.8f, 0.8f))
        glProgram.setFloatUniform("uShadows", shadows.coerceIn(-0.8f, 0.8f))
        glProgram.setFloatUniform("uGlow", beautyGlow.coerceIn(0f, 1f))
        glProgram.setFloatUniform("uSharpen", sharpen.coerceIn(0f, 1f))
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
            uniform float uVignette;
            uniform float uHighlights;
            uniform float uShadows;
            uniform float uGlow;
            uniform float uSharpen;
            varying vec2 vTexSamplingCoord;
            void main() {
              vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
              vec3 rgb = src.rgb;
              if (uSharpen > 0.01) {
                vec3 n1 = texture2D(uTexSampler, clamp(vTexSamplingCoord + vec2(0.002, 0.0), 0.0, 1.0)).rgb;
                vec3 n2 = texture2D(uTexSampler, clamp(vTexSamplingCoord - vec2(0.002, 0.0), 0.0, 1.0)).rgb;
                vec3 n3 = texture2D(uTexSampler, clamp(vTexSamplingCoord + vec2(0.0, 0.002), 0.0, 1.0)).rgb;
                vec3 n4 = texture2D(uTexSampler, clamp(vTexSamplingCoord - vec2(0.0, 0.002), 0.0, 1.0)).rgb;
                vec3 avg = (n1 + n2 + n3 + n4) * 0.25;
                rgb = clamp(rgb + (rgb - avg) * uSharpen, 0.0, 1.0);
              }
              float luma = dot(rgb, vec3(0.299, 0.587, 0.114));

              // Tone curve: Shadows lift & Highlights roll-off
              float shadowWeight = 1.0 - smoothstep(0.0, 0.55, luma);
              float highWeight = smoothstep(0.45, 1.0, luma);
              rgb += shadowWeight * uShadows * 0.25;
              rgb += highWeight * uHighlights * 0.25;

              // Soft cinematic highlight bloom
              rgb += highWeight * uGlow * 0.12;

              // DaVinci / Premiere Vignette
              float dist = distance(vTexSamplingCoord, vec2(0.5, 0.5));
              float vig = 1.0 - smoothstep(0.32, 0.85, dist) * (uVignette * 0.78);
              rgb *= vig;

              gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), src.a);
            }
        """
    }
}
