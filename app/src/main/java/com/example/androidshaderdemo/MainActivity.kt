
package com.example.androidshaderdemo

import android.graphics.RuntimeShader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.androidshaderdemo.ui.theme.AndroidShaderDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidShaderDemoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    var isClicked by remember { mutableStateOf(false) }
                    Column() {
                        Button(
                            modifier = Modifier.size(150.dp),
                            onClick = { isClicked = !isClicked }) {
                            Text("Click me")
                        }
                        Box(modifier = Modifier.size(1000.dp)) {
                            val shader = remember { RuntimeShader(SHADER) }
                            val shaderBrush = ShaderBrush(shader)
                            val time by produceState(0f) {
                                while (true) {
                                    withInfiniteAnimationFrameMillis {
                                        value = it / 1000f
                                    }
                                }
                            }
                            shader.setFloatUniform("iTime", time)
                            val speed = if (isClicked) 0.5f else 10f
                            shader.setFloatUniform("iSpeed", speed)

                            Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
                                shader.setFloatUniform("iResolution", size.width, size.height)
                                drawRect(brush = shaderBrush, size = DpSize(1000.dp, 1000.dp).toSize())
                            })
                        }
                    }
                }
            }
        }
    }
}

private const val SHADER = """
uniform float iTime;
uniform float iSpeed;
uniform vec2 iResolution;

float noise(vec3 p)
{
    vec3 i = floor(p);
    vec4 a = dot(i, vec3(1., 57., 21.)) + vec4(0., 57., 21., 78.);
    vec3 f = cos((p-i)*acos(-1.))*(-.5)+.5;
    a = mix(sin(cos(a)*a),sin(cos(1.+a)*(1.+a)), f.x);
    a.xy = mix(a.xz, a.yw, f.y);
    return mix(a.x, a.y, f.z);
}

float sphere(vec3 p, vec4 spr)
{
    return length(spr.xyz-p) - spr.w;
}

float flame(vec3 p)
{
    float d = sphere(p*vec3(1.,.5,1.), vec4(.0,-1.,.0,1.));
    return d + (noise(p+vec3(.0,iTime*iSpeed,.0)) + noise(p*3.)*.5)*.25*(p.y) ;
}

float scene(vec3 p)
{
    return min(100.-length(p) , abs(flame(p)) );
}

vec4 raymarch(vec3 org, vec3 dir)
{
    float d = 0.0, glow = 0.0, eps = 0.02;
    vec3  p = org;
    bool glowed = false;

    for(int i=0; i<64; i++)
    {
        d = scene(p) + eps;
        p += d * dir;
        if( d>eps )
        {
            if(flame(p) < .0)
                glowed=true;
            if(glowed)
                glow = float(i)/64.;
        }
    }
    return vec4(p,glow);
}

vec4 main(vec2 fragCoord )
{
    vec2 v = -1.0 + 2.0 * fragCoord.xy / iResolution.xy;
    v.x *= iResolution.x/iResolution.y;

    vec3 org = vec3(0., -2., 4.);
    vec3 dir = normalize(vec3(v.x*1.6, -v.y, -1.5));

    vec4 p = raymarch(org, dir);
    float glow = p.w;

    vec4 col = mix(vec4(1.,.5,.1,1.), vec4(0.1,.5,1.,1.), p.y*.02+.4);

    return mix(vec4(0.), col, pow(glow*2.,4.));
}
"""