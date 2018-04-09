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
}
