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
import com.aistudio.kbeditor.r7v4qx.model.VideoFxPreset

@UnstableApi
class VideoFxGlEffect(
    private val fxPreset: VideoFxPreset,
    private val intensity: Float = 1f,
    private val speed: Float = 1f,
    private val colorShift: Float = 0f
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return VideoFxShaderProgram(useHdr, fxPreset)
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return fxPreset == VideoFxPreset.NONE
    }
}

@UnstableApi
private class VideoFxShaderProgram(
    useHdr: Boolean,
    fxPreset: VideoFxPreset
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

        val mode = when (fxPreset) {
            VideoFxPreset.NONE -> 0.0f
            VideoFxPreset.GLITCH_RGB, VideoFxPreset.TEAR_APART, VideoFxPreset.PIXEL_MOSAIC -> 1.0f
            VideoFxPreset.VHS_RETRO, VideoFxPreset.OLD_TV, VideoFxPreset.VINTAGE_FILM,
            VideoFxPreset.RETRO_FLICKER, VideoFxPreset.RETRO_CINEMA, VideoFxPreset.VIDEO_CAM,
            VideoFxPreset.SEPIA_NOIR, VideoFxPreset.LEAK_1, VideoFxPreset.LEAK_2 -> 2.0f
            VideoFxPreset.CAM_SHAKE, VideoFxPreset.EARTHQUAKE, VideoFxPreset.CAM_SWING,
            VideoFxPreset.BOUNCE_FX, VideoFxPreset.REBOUND_SWING, VideoFxPreset.SINGLE_SHOCK,
            VideoFxPreset.PHONE_SWAG, VideoFxPreset.SLIGHT_SHAKE, VideoFxPreset.PENDULUM_SWING -> 3.0f
            VideoFxPreset.FLASH_STROBE, VideoFxPreset.ACTION_FLASH, VideoFxPreset.WHITE_FLASH,
            VideoFxPreset.BLACK_FLASH_2, VideoFxPreset.FLASH_BOUNCE, VideoFxPreset.CAMERA_STROBE,
            VideoFxPreset.DECOLOR_FLASH, VideoFxPreset.FLICKER_BLACK, VideoFxPreset.ZOOM_FLASH -> 4.0f
            VideoFxPreset.CHROMATIC, VideoFxPreset.PRISM_SHIMMER, VideoFxPreset.SHARDS_LIGHT,
            VideoFxPreset.LASER_WIPE, VideoFxPreset.LASER_REVEAL -> 6.0f
            VideoFxPreset.SPLIT_2X, VideoFxPreset.SLICE_BY_SLICE -> 9.0f
            VideoFxPreset.SPLIT_3X, VideoFxPreset.SPLIT_4X, VideoFxPreset.SPLIT_6X, VideoFxPreset.SPLIT_9X -> 10.0f
            VideoFxPreset.NEON_EDGE, VideoFxPreset.NEON_FLOW, VideoFxPreset.CYBER_SCAN,
            VideoFxPreset.MANGA_SHOCK, VideoFxPreset.SKETCH_OUTLINE -> 11.0f
            else -> when (fxPreset.category) {
                "Lens & Blur", "Shake & Beat", "Opening & Closing" -> 7.0f
                "Retro & Film", "Comic & Manga" -> 8.0f
                else -> 5.0f
            }
        }
        glProgram.setFloatUniform("uFxMode", mode)
        glProgram.setFloatUniform("uTimeSec", 0.0f)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setFloatUniform("uTimeSec", presentationTimeUs / 1_000_000.0f)
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
            uniform float uFxMode;
            uniform float uTimeSec;
            varying vec2 vTexSamplingCoord;

            void main() {
                vec2 uv = vTexSamplingCoord;
                int mode = int(uFxMode + 0.5);

                if (mode == 9) {
                    uv.y = fract(uv.y * 2.0);
                } else if (mode == 10) {
                    uv.y = fract(uv.y * 3.0);
                } else if (mode == 3) {
                    uv.x = clamp(uv.x + sin(uTimeSec * 28.0) * 0.015, 0.0, 1.0);
                    uv.y = clamp(uv.y + cos(uTimeSec * 22.0) * 0.012, 0.0, 1.0);
                } else if (mode == 7) {
                    float z = 1.0 + 0.08 * abs(sin(uTimeSec * 8.0));
                    uv = (uv - 0.5) / z + 0.5;
                }

                vec4 col = texture2D(uTexSampler, uv);

                if (mode == 1 || mode == 6) {
                    float off = (mode == 1) ? 0.014 * sin(uTimeSec * 24.0) : 0.009;
                    float r = texture2D(uTexSampler, clamp(uv + vec2(off, 0.0), 0.0, 1.0)).r;
                    float b = texture2D(uTexSampler, clamp(uv - vec2(off, 0.0), 0.0, 1.0)).b;
                    col = vec4(r, col.g, b, col.a);
                } else if (mode == 2) {
                    float scan = sin(uv.y * 240.0) * 0.08;
                    col.rgb = clamp(col.rgb - scan, 0.0, 1.0);
                } else if (mode == 4) {
                    float flash = step(0.5, fract(uTimeSec * 4.5)) * 0.25;
                    col.rgb = clamp(col.rgb + flash, 0.0, 1.0);
                } else if (mode == 5) {
                    col.rgb = clamp(col.rgb * 1.12 + vec3(0.05, 0.04, 0.08), 0.0, 1.0);
                } else if (mode == 8) {
                    float grain = fract(sin(dot(uv * (uTimeSec + 1.0), vec2(12.9898, 78.233))) * 43758.5453) * 0.06;
                    col.rgb = clamp(col.rgb + grain - 0.03, 0.0, 1.0);
                } else if (mode == 11) {
                    float border = step(uv.x, 0.02) + step(0.98, uv.x) + step(uv.y, 0.02) + step(0.98, uv.y);
                    if (border > 0.5) {
                        col.rgb = mix(col.rgb, vec3(0.13, 0.83, 0.93), 0.85);
                    }
                }

                gl_FragColor = col;
            }
        """
    }
}
