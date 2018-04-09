/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package is.ulrik.autovectorisationtest;

import cleargl.GLMatrix;
import com.jogamp.opengl.math.FloatUtil;
import org.openjdk.jmh.annotations.*;

import java.util.Random;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

@State(Scope.Thread)
@OutputTimeUnit(NANOSECONDS)
@BenchmarkMode(AverageTime)
@Fork(value = 1, jvmArgsAppend = {
        "-XX:-UseSuperWord",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:CompileCommand=print,*BenchmarkSIMDBlog.array1"})
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class MultiplyMatrices {

  @State(Scope.Thread)
  public static class Context {
    // input matrices
    public final GLMatrix A = new GLMatrix();
    public final GLMatrix B = new GLMatrix();
    // result matrix
    public GLMatrix C = new GLMatrix();
    public static final GLMatrix zero = new GLMatrix(new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f});

    @Setup
    public void setup() {
      C = zero.clone();
      Random r = new Random();
      for(int i = 0; i < 16; i++) {
        A.getFloatArray()[i] = r.nextFloat();
        B.getFloatArray()[i] = r.nextFloat();
      }
    }

  }

  @Benchmark
  public void multiplyMatricesFloatUtilGLMatrix(Context context) {
    FloatUtil.multMatrix(context.A.getFloatArray(), context.B.getFloatArray(), context.C.getFloatArray());
  }

  @Benchmark
  public void multiplyMatricesLoop(Context context) {
    float sum = 0.0f;
    for(int i = 0; i < 4; i++) {
      for(int j = 0; j < 4; j++) {
        sum = 0.0f;

        for(int k = 0; k < 4; k++) {
          sum += context.A.getFloatArray()[i*4 + k] * context.B.getFloatArray()[k*4 + j];
        }

        context.C.getFloatArray()[i * 4 + j] = sum;
      }
    }
  }

  @Benchmark
  public void multiplyMatricesLoopFMA(Context context) {
    float sum = 0.0f;
    for(int i = 0; i < 4; i++) {
      for(int j = 0; j < 4; j++) {
        sum = 0.0f;
        for(int k = 0; k < 4; k++) {
          // Math.fma(a,b,c) = a * b + c
          sum = Math.fma(context.A.getFloatArray()[i*4 + k], context.B.getFloatArray()[k*4 + j], sum);
        }
        context.C.getFloatArray()[i * 4 + j] = sum;
      }
    }
  }

  @Benchmark
  public void multiplyMatricesFloatUtilFMA(Context context) {
    multMatrixFMA(context.A.getFloatArray(), context.B.getFloatArray(), context.C.getFloatArray());
  }

  @Benchmark
  public void multiplyMatricesFloatUtilFMALoop(Context context) {
    multMatrixFMALoop(context.A.getFloatArray(), context.B.getFloatArray(), context.C.getFloatArray());
  }

  public static float[] multMatrixFMA(final float[] a, final float[] b, final float[] d) {
    final float b00 = b[0+0*4];
    final float b10 = b[1+0*4];
    final float b20 = b[2+0*4];
    final float b30 = b[3+0*4];
    final float b01 = b[0+1*4];
    final float b11 = b[1+1*4];
    final float b21 = b[2+1*4];
    final float b31 = b[3+1*4];
    final float b02 = b[0+2*4];
    final float b12 = b[1+2*4];
    final float b22 = b[2+2*4];
    final float b32 = b[3+2*4];
    final float b03 = b[0+3*4];
    final float b13 = b[1+3*4];
    final float b23 = b[2+3*4];
    final float b33 = b[3+3*4];

    float ai0=a[  0*4]; // row-0 of a
    float ai1=a[  1*4];
    float ai2=a[  2*4];
    float ai3=a[  3*4];
    d[ 0*4] = Math.fma(ai0, b00, Math.fma(ai1, b10, Math.fma(ai2, b20, ai3*b30)));
    d[ 1*4] = Math.fma(ai0, b01, Math.fma(ai1, b11, Math.fma(ai2, b21, ai3*b31)));
    d[ 2*4] = Math.fma(ai0, b02, Math.fma(ai1, b12, Math.fma(ai2, b22, ai3*b32)));
    d[ 3*4] = Math.fma(ai0, b03, Math.fma(ai1, b13, Math.fma(ai2, b23, ai3*b33)));

    ai0=a[1+0*4]; // row-1 of a
    ai1=a[1+1*4];
    ai2=a[1+2*4];
    ai3=a[1+3*4];
    d[1+0*4] = Math.fma(ai0, b00, Math.fma(ai1, b10, Math.fma(ai2, b20, ai3*b30)));
    d[1+1*4] = Math.fma(ai0, b01, Math.fma(ai1, b11, Math.fma(ai2, b21, ai3*b31)));
    d[1+2*4] = Math.fma(ai0, b02, Math.fma(ai1, b12, Math.fma(ai2, b22, ai3*b32)));
    d[1+3*4] = Math.fma(ai0, b03, Math.fma(ai1, b13, Math.fma(ai2, b23, ai3*b33)));

    ai0=a[2+0*4]; // row-2 of a
    ai1=a[2+1*4];
    ai2=a[2+2*4];
    ai3=a[2+3*4];
    d[2+0*4] = Math.fma(ai0, b00, Math.fma(ai1, b10, Math.fma(ai2, b20, ai3*b30)));
    d[2+1*4] = Math.fma(ai0, b01, Math.fma(ai1, b11, Math.fma(ai2, b21, ai3*b31)));
    d[2+2*4] = Math.fma(ai0, b02, Math.fma(ai1, b12, Math.fma(ai2, b22, ai3*b32)));
    d[2+3*4] = Math.fma(ai0, b03, Math.fma(ai1, b13, Math.fma(ai2, b23, ai3*b33)));

    ai0=a[3+0*4]; // row-3 of a
    ai1=a[3+1*4];
    ai2=a[3+2*4];
    ai3=a[3+3*4];
    d[3+0*4] = Math.fma(ai0, b00, Math.fma(ai1, b10, Math.fma(ai2, b20, ai3*b30)));
    d[3+1*4] = Math.fma(ai0, b01, Math.fma(ai1, b11, Math.fma(ai2, b21, ai3*b31)));
    d[3+2*4] = Math.fma(ai0, b02, Math.fma(ai1, b12, Math.fma(ai2, b22, ai3*b32)));
    d[3+3*4] = Math.fma(ai0, b03, Math.fma(ai1, b13, Math.fma(ai2, b23, ai3*b33)));

    return d;
  }

  public static float[] multMatrixFMALoop(final float[] a, final float[] b, final float[] d) {
    final float b00 = b[0+0*4];
    final float b10 = b[1+0*4];
    final float b20 = b[2+0*4];
    final float b30 = b[3+0*4];
    final float b01 = b[0+1*4];
    final float b11 = b[1+1*4];
    final float b21 = b[2+1*4];
    final float b31 = b[3+1*4];
    final float b02 = b[0+2*4];
    final float b12 = b[1+2*4];
    final float b22 = b[2+2*4];
    final float b32 = b[3+2*4];
    final float b03 = b[0+3*4];
    final float b13 = b[1+3*4];
    final float b23 = b[2+3*4];
    final float b33 = b[3+3*4];

    for(int i = 0; i < 4; i++) {
      float ai0 = a[i+0 * 4]; // row-0 of a
      float ai1 = a[i+1 * 4];
      float ai2 = a[i+2 * 4];
      float ai3 = a[i+3 * 4];
      d[i+0 * 4] = Math.fma(ai0, b00, Math.fma(ai1, b10, Math.fma(ai2, b20, ai3 * b30)));
      d[i+1 * 4] = Math.fma(ai0, b01, Math.fma(ai1, b11, Math.fma(ai2, b21, ai3 * b31)));
      d[i+2 * 4] = Math.fma(ai0, b02, Math.fma(ai1, b12, Math.fma(ai2, b22, ai3 * b32)));
      d[i+3 * 4] = Math.fma(ai0, b03, Math.fma(ai1, b13, Math.fma(ai2, b23, ai3 * b33)));
    }

    return d;
  }
}
