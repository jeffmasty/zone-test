package judahzone.dev;

//import org.apache.commons.math3.complex.Complex;
//import org.bytedeco.fftw.global.fftw3;
//import org.bytedeco.javacpp.DoublePointer;

public class FFTWWrappers {
//  public static class FFTW_R2C_1D_Executor {
//    public int input_size;
//    private DoublePointer input_buffer;
//    public int output_size;
//    private DoublePointer output_buffer;
//    private fftw3.fftw_plan plan;
//
//    public FFTW_R2C_1D_Executor(int n_real_samples) {
//      this.input_size = n_real_samples;
//      this.input_buffer = fftw3.fftw_alloc_real(this.input_size);
//      this.output_size = n_real_samples / 2 + 1;
//      this.output_buffer = fftw3.fftw_alloc_complex(this.output_size);
//      this.plan =
//          fftw3.fftw_plan_dft_r2c_1d(
//              this.input_size, this.input_buffer, this.output_buffer, fftw3.FFTW_ESTIMATE);
//    }
//
//    public void free() {
//      fftw3.fftw_destroy_plan(this.plan);
//      fftw3.fftw_free(this.input_buffer);
//      fftw3.fftw_free(this.output_buffer);
//    }
//
//    public void set_input_zeropadded(double[] buffer) {
//      int size = buffer.length;
//      assert (size <= this.input_size);
//      // The DoublePointer type allows for C style memset
//      // and memcpy calls, which were used in the C++
//      // example.  Preserving this logic would seem to require
//      // first creating a DoublePointer from the Java array
//      // (buffer), which involves an additional copy operation.
//      // An alternative might be to use
//      // this.input_buffer.put(buffer) instead of memcpy,
//      // which would still require a memset call or similar.
//      // We use the memcpy approach to more closely follow
//      // the original example.
//      DoublePointer.memcpy(
//          this.input_buffer, new DoublePointer(buffer), this.input_buffer.sizeof() * size);
//      DoublePointer.memset(
//          this.input_buffer.getPointer(size),
//          0,
//          this.input_buffer.sizeof() * (this.input_size - size));
//    }
//
//    public void execute() {
//      fftw3.fftw_execute(plan);
//    }
//
//    public DoublePointer get_input_pointer() {
//      return this.input_buffer;
//    }
//
//    public double[] get_output() {
//      // multiply by 2 as this is an array of doubles
//      // and not complex numbers
//      double[] result = new double[2 * this.output_size];
//      this.output_buffer.get(result);
//      return result;
//    }
//
//    public DoublePointer get_output_pointer() {
//      return this.output_buffer;
//    }
//
//    public Complex[] get_output_as_complex_array() {
//      Complex[] result = new Complex[this.output_size];
//      double[] ds = new double[2 * this.output_size];
//      this.output_buffer.get(ds);
//      for (int i = 0; i < result.length; i++) {
//        result[i] = new Complex(ds[2 * i], ds[2 * i + 1]);
//      }
//      return result;
//    }
//  };
//
//  public static class FFTW_C2R_1D_Executor {
//    public int input_size;
//    private DoublePointer input_buffer;
//    public int output_size;
//    private DoublePointer output_buffer;
//    private fftw3.fftw_plan plan;
//
//    public FFTW_C2R_1D_Executor(int n_real_samples) {
//      this.input_size = n_real_samples / 2 + 1;
//      this.input_buffer = fftw3.fftw_alloc_complex(this.input_size);
//      this.output_size = n_real_samples;
//      this.output_buffer = fftw3.fftw_alloc_real(this.output_size);
//      this.plan =
//          fftw3.fftw_plan_dft_c2r_1d(
//              this.output_size, this.input_buffer, this.output_buffer, fftw3.FFTW_ESTIMATE);
//    }
//
//    public void free() {
//      fftw3.fftw_destroy_plan(this.plan);
//      fftw3.fftw_free(this.input_buffer);
//      fftw3.fftw_free(this.output_buffer);
//    }
//
//    public void set_input(DoublePointer ptr, int size) {
//      assert (size == this.input_size);
//      DoublePointer.memcpy(
//          this.input_buffer, ptr, 2 * ptr.sizeof() * size); // 2 for sizeof(complex)/sizeof(double)
//      DoublePointer.memset(
//          this.input_buffer.getPointer(size), 0, ptr.sizeof() * (this.input_size - size));
//    }
//
//    public void set_input(double[] buffer) {
//      assert ((buffer.length / 2) == this.input_size);
//      // Comments above about memcpy also apply here.
//      DoublePointer.memcpy(
//          this.input_buffer, new DoublePointer(buffer), this.input_buffer.sizeof() * buffer.length);
//      DoublePointer.memset(
//          this.input_buffer.getPointer(buffer.length),
//          0,
//          this.input_buffer.sizeof() * (2 * input_size - buffer.length));
//    }
//
//    public void set_input(Complex[] buffer) {
//      assert (buffer.length == this.input_size);
//      double[] buffer_reals = new double[2 * buffer.length];
//      for (int i = 0; i < buffer.length; i++) {
//        buffer_reals[2 * i] = buffer[i].getReal();
//        buffer_reals[2 * i + 1] = buffer[i].getImaginary();
//      }
//      this.set_input(buffer_reals);
//    }
//
//    public void execute() {
//      fftw3.fftw_execute(plan);
//    }
//
//    public DoublePointer get_output_ponter() {
//      return this.output_buffer;
//    }
//
//    public double[] get_output() {
//      double[] result = new double[this.output_size];
//      this.output_buffer.get(result);
//      return result;
//    }
//  };
};



// test various FFT libraries by running them against files many times and timing the results.

// RUN: forward transform + magnitudes (modulus)

/* equivalent:
 *
	System.arraycopy(buffer.getLeft(), 0, transformBuffer, 0, FFT_SIZE); // mono
	fft.forwardTransform(transformBuffer);
	float[] amplitudes = new float[AMPLITUDES];
	fft.modulus(transformBuffer, amplitudes);

 */


/*
//JTransforms fields (add imports: org.jtransforms.fft.DoubleFFT_1D)
private final org.jtransforms.fft.DoubleFFT_1D jfft = new org.jtransforms.fft.DoubleFFT_1D(FFT_SIZE);
private final double[] jtInterleaved = new double[FFT_SIZE * 2]; // re,im,re,im,...
private final float[] jtAmplitudes = new float[AMPLITUDES];     // reusable output
private final boolean jtNormalize = false; // set true if you want amplitudes /= FFT_SIZE

*/

/*
// copy mono samples into interleaved double buffer (real,imag interleaved)
float[] left = buffer.getLeft(); // length = FFT_SIZE
for (int i = 0, j = 0; i < FFT_SIZE; i++, j += 2) {
    jtInterleaved[j] = left[i];
    jtInterleaved[j + 1] = 0.0d;
}

// forward in-place complex FFT
jfft.complexForward(jtInterleaved);

// compute half-spectrum magnitudes (bins 0 .. FFT_SIZE/2 - 1)
for (int k = 0, j = 0; k < AMPLITUDES; k++, j += 2) {
    double re = jtInterleaved[j];
    double im = jtInterleaved[j + 1];
    double mag = Math.hypot(re, im); // stable hypot
    if (jtNormalize) mag /= (double) FFT_SIZE;
    jtAmplitudes[k] = (float) mag;
}

// jtAmplitudes now contains the amplitudes (use it instead of allocating a new array)
float[] amplitudes = jtAmplitudes;
*/
/**
// fields (create once)
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.complex.Complex;

private final FastFourierTransformer acmFft =
new FastFourierTransformer(DftNormalization.STANDARD);

//per-frame: equivalent to your Tarsos code
double[] real = new double[FFT_SIZE];
float[] left = buffer.getLeft(); // float[] from your Recording
for (int i = 0; i < FFT_SIZE; i++) {
real[i] = left[i]; // copy and convert to double
}

//forward transform -> Complex[] (length == FFT_SIZE)
Complex[] spec = acmFft.transform(real, TransformType.FORWARD);

//extract half-spectrum magnitudes (0 .. FFT_SIZE/2-1)
float[] amplitudes = new float[AMPLITUDES];
for (int k = 0; k < AMPLITUDES; k++) {
amplitudes[k] = (float) spec[k].abs();
}

//Note: Apache Commons Math uses its own normalization mode (STANDARD/UNITARY).
//If your downstream expects a different scaling, adjust by dividing/multiplying
//by FFT_SIZE or sqrt(FFT_SIZE) accordingly.
JTransforms (recommended for performance)
Operates in-place on a double[] interleaved [re,im,re,im,...] buffer
Avoids Complex allocations and is very fast; use DoubleFFT_1D once and reuse
Matches your existing transformBuffer layout if you use TRANSFORM = FFT_SIZE*2
Java
//fields (create once)
import org.jtransforms.fft.DoubleFFT_1D;

private final DoubleFFT_1D jfft = new DoubleFFT_1D(FFT_SIZE);

//per-frame: high-performance path
float[] left = buffer.getLeft(); // float[] length FFT_SIZE

//reuse a double[] interleaved buffer (allocate once as a field for best perf)
double[] interleaved = new double[FFT_SIZE * 2]; // re,im, re,im, ...
//copy real samples into interleaved real slots, zero imag
for (int i = 0, j = 0; i < FFT_SIZE; i++, j += 2) {
interleaved[j] = left[i];
interleaved[j + 1] = 0.0;
}

//compute complex forward in-place
jfft.complexForward(interleaved);

//compute half-spectrum magnitudes (bins 0 .. FFT_SIZE/2-1)
float[] amplitudes = new float[AMPLITUDES];
for (int k = 0; k < AMPLITUDES; k++) {
int j = k * 2;
double re = interleaved[j];
double im = interleaved[j + 1];
amplitudes[k] = (float) Math.hypot(re, im); // stable hypot(re,im)
}

//Note: JTransforms does not scale the forward transform. If you need
//the same amplitude scaling as another library, divide amplitudes by FFT_SIZE.*/
