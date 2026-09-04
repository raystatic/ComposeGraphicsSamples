package com.example.graphicspoc

import android.graphics.LinearGradient
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb

private const val DIR_BLUR_AGSL =
    """
    uniform shader u_input;
    uniform float2 u_resolution;
    uniform float u_radius;
    uniform float u_angle;
    half4 main(float2 fc) {
        if(u_radius<1.0) return u_input.eval(fc);
        float2 dir=float2(cos(radians(u_angle)),sin(radians(u_angle)));
        const int S=13; float sig=u_radius/2.5;
        half4 c=half4(0.0); float tw=0.0;
        float rnd=fract(sin(dot(fc,float2(12.9898,78.233)))*43758.5453);
        for(int i=0;i<S;i++){
            float t=float(i-S/2);
            float o=(t+(rnd-0.5))*(u_radius/float(S/2));
            float w=exp(-(t*t)/(2.0*sig*sig));
            c+=u_input.eval(fc+dir*o)*w; tw+=w;
        }
        return c/tw;
    }
"""


const val LAYER_AGSL_SRC =
    """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform int u_isDark;
    uniform shader u_gradient;
    uniform float u_scrollSpeed;
    uniform float u_gradientZoom;
    uniform float u_uvRotation;
    uniform int u_reverseScroll;
    uniform float u_colorOffset;
    uniform float2 u_shapeCenter;
    uniform float2 u_shapeRadius;
    uniform float u_shapeSoftness;
    uniform float u_shapeOpacity;
    uniform int u_enableNoiseMask;
    uniform float u_noiseMaskScale;
    uniform float u_noiseMaskSpeed;
    uniform int u_noiseMaskDetail;
    uniform float u_noiseMaskStrength;
    uniform float u_noiseMaskAngle;
    uniform float u_noiseMaskRotation;
    uniform float u_brightness;
    uniform float u_contrast;
    uniform float u_dispAmount;
    uniform float u_dispScale;
    uniform float u_evoSpeed;
    uniform int u_noiseDetail;

    float2 mod289(float2 x) { return x - floor(x * (1.0/289.0)) * 289.0; }
    float3 mod289(float3 x) { return x - floor(x * (1.0/289.0)) * 289.0; }
    float3 perm(float3 x) { return mod289(((x*34.0)+1.0)*x); }

    float snoise2D(float2 v) {
        const float4 C = float4(0.211324865405187,0.366025403784439,-0.577350269189626,0.024390243902439);
        float2 i = floor(v+dot(v,C.yy));
        float2 x0 = v-i+dot(i,C.xx);
        float2 i1 = (x0.x>x0.y)?float2(1,0):float2(0,1);
        float4 x12 = x0.xyxy+C.xxzz; x12.xy -= i1;
        i = mod289(i);
        float3 p = perm(perm(i.y+float3(0,i1.y,1))+i.x+float3(0,i1.x,1));
        float3 m = max(float3(0.5)-float3(dot(x0,x0),dot(x12.xy,x12.xy),dot(x12.zw,x12.zw)),0.0);
        m=m*m; m=m*m;
        float3 x = 2.0*fract(p*C.www)-1.0;
        float3 h = abs(x)-0.5;
        float3 ox = floor(x+0.5);
        float3 a0 = x-ox;
        m *= 1.79284291400159-0.85373472095314*(a0*a0+h*h);
        float3 g; g.x=a0.x*x0.x+h.x*x0.y; g.yz=a0.yz*x12.xz+h.yz*x12.yw;
        return 130.0*dot(m,g);
    }

    float fbm2D(float2 c, int oct) {
        float v=0.0,a=0.5;
        for(int i=0;i<6;i++){if(i>=oct)break;v+=a*snoise2D(c);c*=2.0;a*=0.5;}
        return v;
    }

    float hash(float2 p) { return fract(sin(dot(p,float2(127.1,311.7)))*43758.5453); }
    float vnoise(float2 p) {
        float2 i=floor(p),f=fract(p);
        f=f*f*(3.0-2.0*f);
        return mix(mix(hash(i),hash(i+float2(1,0)),f.x),mix(hash(i+float2(0,1)),hash(i+float2(1,1)),f.x),f.y);
    }
    float fbmH(float2 c, float t, int oct) {
        float v=0.0,a=0.5;
        for(int i=0;i<6;i++){if(i>=oct)break;v+=a*vnoise(c+t*0.3);c*=2.0;a*=0.5;t*=1.4;}
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord/u_resolution;
        uv.y = 1.0-uv.y;
        float ar = u_resolution.x/u_resolution.y;
        float2 nc = uv*(u_resolution/u_dispScale);
        float evo = u_time*u_evoSpeed;
        float dx = fbmH(nc, evo, u_noiseDetail)-0.5;
        float dy = fbmH(nc+100.0, evo+50.0, u_noiseDetail)-0.5;
        float2 duv = uv+float2(dx,dy)*(u_dispAmount/u_resolution);
        float2 suv = duv;
        if(abs(u_uvRotation)>0.01){
            float r=radians(u_uvRotation);
            float2x2 rm=float2x2(cos(r),-sin(r),sin(r),cos(r));
            suv=rm*(duv-0.5)+0.5;
        }
        float sd = u_reverseScroll==1?-1.0:1.0;
        half4 col = u_gradient.eval(float2(fract(suv.x/u_gradientZoom-u_time*u_scrollSpeed*sd+u_colorOffset),0.5));
        float2 d = duv-u_shapeCenter; d.x*=ar;
        float2 rc = u_shapeRadius; rc.x*=ar;
        float dist = length(d/max(rc,float2(0.001)));
        float pe = 2.0/min(u_resolution.x,u_resolution.y);
        float aa = max(u_shapeSoftness,pe*1.5);
        float sm = 1.0-smoothstep(1.0-aa,1.0+aa*0.5,dist);
        if(u_enableNoiseMask==1 && u_noiseMaskStrength>0.0){
            float2 nuv = duv*u_noiseMaskScale;
            float rot=radians(u_noiseMaskRotation);
            float2x2 rm=float2x2(cos(rot),-sin(rot),sin(rot),cos(rot));
            nuv=rm*(nuv-0.5)+0.5;
            float nr=radians(u_noiseMaskAngle);
            float2 no=float2(cos(nr),sin(nr))*u_time*u_noiseMaskSpeed;
            float n=fbm2D(nuv+no,u_noiseMaskDetail)*0.5+0.5;
            sm*=mix(1.0,n,u_noiseMaskStrength);
        }
        sm=(sm-0.5)*u_contrast+0.5+u_brightness;
        sm=clamp(sm,0.0,1.0)*u_shapeOpacity;
        return half4(half3(col.rgb*sm),sm);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NorthernLightsShaderEngine(
    isDark: Boolean = false,
    isVisible: Boolean = true,
    scrollSpeed: Float = 0.206f,
    gradientZoom: Float = 6.85f,
    uvRotation: Float = -45f,
    reverseScroll: Boolean = false,
    bgColorOffset: Float = 0.09f,
    showBG: Boolean = true,
    bgCenterX: Float = 0.48f,
    bgCenterY: Float = 1.10f,
    bgRadiusX: Float = 1.83f,
    bgRadiusY: Float = 0.42f,
    bgSoftness: Float = 2.0f,
    bgOpacity: Float = 1.00f,
    bgBlur: Float = 320.35f,
    bgDirBlur: Float = 0.0f,
    bgDirAngle: Float = -49f,
    showFG: Boolean = true,
    fgCenterX: Float = 0.475f,
    fgCenterY: Float = 0.81f,
    fgRadiusX: Float = 1.30f,
    fgRadiusY: Float = 0.46f,
    fgSoftness: Float = 0.97f,
    fgOpacity: Float = 0.26f,
    fgBlur: Float = 174.96f,
    fgDirBlur: Float = 212f,
    fgDirAngle: Float = -76f,
    noiseMaskScale: Float = 1.396f,
    noiseMaskSpeed: Float = 0.609f,
    noiseMaskDetail: Int = 3,
    noiseMaskStrength: Float = 0.677f,
    noiseMaskAngle: Float = 143.0f,
    noiseMaskRotation: Float = -15.6f,
    brightness: Float = 0.354f,
    contrast: Float = 3f,
    dispAmount: Float = 69f,
    dispScale: Float = 3586f,
    evoSpeed: Float = 0.16f,
    noiseDetail: Int = 3,
    masterAlpha: Float = 1f,
) {
    var fadeAlpha by remember { mutableFloatStateOf(if (isVisible) 1f else 0f) }
    val currentIsVisibleState = rememberUpdatedState(isVisible)
    var animTime by remember { mutableFloatStateOf(0f) }

    val elapsedSeconds by produceState(0f) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos == 0L) lastFrameNanos = now
                val dt = ((now - lastFrameNanos) / 1e9f).coerceAtMost(0.1f)
                lastFrameNanos = now
                val target = if (currentIsVisibleState.value) 1f else 0f
                fadeAlpha += (target - fadeAlpha) * (dt * 2f).coerceAtMost(1f)
                animTime += dt * 0.6f
                value = animTime
            }
        }
    }

    val effectiveAlpha = fadeAlpha * masterAlpha
    if (effectiveAlpha < 0.001f) return

    val bgShader = remember { RuntimeShader(LAYER_AGSL_SRC) }
    val fgShader = remember { RuntimeShader(LAYER_AGSL_SRC) }
    val dirBlurShader = remember { RuntimeShader(DIR_BLUR_AGSL) }

    val gradientShader = remember(isDark) {
        val stops = if (isDark) DARK_STOPS else LIGHT_STOPS
        val colorInts = stops.map { it.second.toArgb() }.toIntArray()
        val stopsArray = stops.map { it.first }.toFloatArray()
        LinearGradient(
            0f, 0f, 1f, 0f,
            colorInts, stopsArray,
            Shader.TileMode.REPEAT,
        )
    }

    val bgFill = if (isDark) Color(0xFF131314) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = bgFill) }

        if (showBG) {
            val bgBlurEffect = remember(bgBlur, bgDirBlur, bgDirAngle) {
                buildLayerBlurEffect(bgBlur / 2f, bgDirBlur / 2f, bgDirAngle, dirBlurShader)
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        bgBlurEffect?.let { renderEffect = it.asComposeRenderEffect() }
                        alpha = effectiveAlpha
                    }
            ) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                setLayerShaderUniforms(
                    shader = bgShader, gradient = gradientShader, w = size.width, h = size.height,
                    elapsedSeconds = elapsedSeconds, isDark = isDark, scrollSpeed = scrollSpeed,
                    gradientZoom = gradientZoom, uvRotation = uvRotation, reverseScroll = reverseScroll,
                    colorOffset = bgColorOffset, centerX = bgCenterX, centerY = bgCenterY,
                    radiusX = bgRadiusX, radiusY = bgRadiusY, softness = bgSoftness, opacity = bgOpacity,
                    enableNoiseMask = false, noiseMaskScale = 0f, noiseMaskSpeed = 0f, noiseMaskDetail = 1,
                    noiseMaskStrength = 0f, noiseMaskAngle = 0f, noiseMaskRotation = 0f, brightness = brightness,
                    contrast = contrast, dispAmount = dispAmount, dispScale = dispScale, evoSpeed = evoSpeed,
                    noiseDetail = noiseDetail
                )
                drawRect(brush = ShaderBrush(bgShader))
            }
        }

        if (showFG) {
            val fgBlurEffect = remember(fgBlur, fgDirBlur, fgDirAngle) {
                buildLayerBlurEffect(fgBlur / 2f, fgDirBlur / 2f, fgDirAngle, dirBlurShader)
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        fgBlurEffect?.let { renderEffect = it.asComposeRenderEffect() }
                        alpha = effectiveAlpha
                    }
            ) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                setLayerShaderUniforms(
                    shader = fgShader, gradient = gradientShader, w = size.width, h = size.height,
                    elapsedSeconds = elapsedSeconds, isDark = isDark, scrollSpeed = scrollSpeed,
                    gradientZoom = gradientZoom, uvRotation = uvRotation, reverseScroll = reverseScroll,
                    colorOffset = 0f, centerX = fgCenterX, centerY = fgCenterY,
                    radiusX = fgRadiusX, radiusY = fgRadiusY, softness = fgSoftness, opacity = fgOpacity,
                    enableNoiseMask = true, noiseMaskScale = noiseMaskScale, noiseMaskSpeed = noiseMaskSpeed,
                    noiseMaskDetail = noiseMaskDetail, noiseMaskStrength = noiseMaskStrength,
                    noiseMaskAngle = noiseMaskAngle, noiseMaskRotation = noiseMaskRotation,
                    brightness = brightness, contrast = contrast, dispAmount = dispAmount,
                    dispScale = dispScale, evoSpeed = evoSpeed, noiseDetail = noiseDetail
                )
                drawRect(brush = ShaderBrush(fgShader))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun buildLayerBlurEffect(
    gaussianPx: Float,
    dirPx: Float,
    dirAngle: Float,
    dirBlurShader: RuntimeShader,
): RenderEffect? {
    val hasGaussian = gaussianPx > 0.5f
    val hasDir = dirPx > 0.5f
    if (!hasGaussian && !hasDir) return null

    val gaussEffect = if (hasGaussian) {
        RenderEffect.createBlurEffect(
            gaussianPx.coerceAtLeast(0.1f),
            gaussianPx.coerceAtLeast(0.1f),
            Shader.TileMode.DECAL,
        )
    } else null

    val dirEffect = if (hasDir) {
        dirBlurShader.setFloatUniform("u_resolution", 1080f, 2400f)
        dirBlurShader.setFloatUniform("u_radius", dirPx)
        dirBlurShader.setFloatUniform("u_angle", dirAngle)
        RenderEffect.createRuntimeShaderEffect(dirBlurShader, "u_input")
    } else null

    return when {
        gaussEffect != null && dirEffect != null -> RenderEffect.createChainEffect(gaussEffect, dirEffect)
        gaussEffect != null -> gaussEffect
        dirEffect != null -> dirEffect
        else -> null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setLayerShaderUniforms(
    shader: RuntimeShader,
    gradient: LinearGradient,
    w: Float, h: Float,
    elapsedSeconds: Float,
    isDark: Boolean,
    scrollSpeed: Float,
    gradientZoom: Float,
    uvRotation: Float,
    reverseScroll: Boolean,
    colorOffset: Float,
    centerX: Float, centerY: Float,
    radiusX: Float, radiusY: Float,
    softness: Float, opacity: Float,
    enableNoiseMask: Boolean,
    noiseMaskScale: Float,
    noiseMaskSpeed: Float,
    noiseMaskDetail: Int,
    noiseMaskStrength: Float,
    noiseMaskAngle: Float,
    noiseMaskRotation: Float,
    brightness: Float,
    contrast: Float,
    dispAmount: Float,
    dispScale: Float,
    evoSpeed: Float,
    noiseDetail: Int,
) {
    shader.setFloatUniform("u_resolution", w, h)
    shader.setFloatUniform("u_time", elapsedSeconds)
    shader.setIntUniform("u_isDark", if (isDark) 1 else 0)
    shader.setInputShader("u_gradient", gradient)
    shader.setFloatUniform("u_scrollSpeed", scrollSpeed)
    shader.setFloatUniform("u_gradientZoom", gradientZoom)
    shader.setFloatUniform("u_uvRotation", uvRotation)
    shader.setIntUniform("u_reverseScroll", if (reverseScroll) 1 else 0)
    shader.setFloatUniform("u_colorOffset", colorOffset)
    shader.setFloatUniform("u_shapeCenter", centerX, centerY)
    shader.setFloatUniform("u_shapeRadius", radiusX, radiusY)
    shader.setFloatUniform("u_shapeSoftness", softness)
    shader.setFloatUniform("u_shapeOpacity", opacity)
    shader.setIntUniform("u_enableNoiseMask", if (enableNoiseMask) 1 else 0)
    shader.setFloatUniform("u_noiseMaskScale", noiseMaskScale)
    shader.setFloatUniform("u_noiseMaskSpeed", noiseMaskSpeed)
    shader.setIntUniform("u_noiseMaskDetail", noiseMaskDetail)
    shader.setFloatUniform("u_noiseMaskStrength", noiseMaskStrength)
    shader.setFloatUniform("u_noiseMaskAngle", noiseMaskAngle)
    shader.setFloatUniform("u_noiseMaskRotation", noiseMaskRotation)
    shader.setFloatUniform("u_brightness", brightness)
    shader.setFloatUniform("u_contrast", contrast)
    shader.setFloatUniform("u_dispAmount", dispAmount)
    shader.setFloatUniform("u_dispScale", dispScale)
    shader.setFloatUniform("u_evoSpeed", evoSpeed)
    shader.setIntUniform("u_noiseDetail", noiseDetail)
}
