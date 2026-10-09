package com.combatcore.module.setting;

/**
 * Integer setting with a closed range {@code [min, max]} and a step used to
 * snap values (sliders and config input). Optionally carries a display suffix
 * such as {@code %} or {@code t} (ticks).
 */
public class NumberSetting extends Setting<Integer> {

	private final int min;
	private final int max;
	private final int step;
	private final String suffix;

	public NumberSetting(String name, int defaultValue, int min, int max) {
		this(name, defaultValue, min, max, 1, "");
	}

	public NumberSetting(String name, int defaultValue, int min, int max, String suffix) {
		this(name, defaultValue, min, max, 1, suffix);
	}

	public NumberSetting(String name, int defaultValue, int min, int max, int step, String suffix) {
		super(name, clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
		this.step = Math.max(1, step);
		this.suffix = suffix == null ? "" : suffix;
		// Setting's constructor skips sanitize() (it would run before min/max
		// exist), so run the default through it now that the fields are set.
		setValue(Integer.valueOf(clamp(defaultValue, min, max)));
	}

	@Override
	protected Integer sanitize(Integer value) {
		if (value == null) {
			return clamp(this.min, this.min, this.max);
		}

		return clamp(value, this.min, this.max);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public int getMin() {
		return this.min;
	}

	public int getMax() {
		return this.max;
	}

	public int getStep() {
		return this.step;
	}

	public String getSuffix() {
		return this.suffix;
	}

	/** {@code "Chance: 100%"} - the label used by the ClickGUI slider. */
	public String getDisplayString() {
		return this.getName() + ": " + this.getValue() + this.suffix;
	}

	/** Normalised position of the current value inside {@code [min, max]}. */
	public float getFraction() {
		if (this.max <= this.min) {
			return 0.0F;
		}

		return (this.getValue() - this.min) / (float) (this.max - this.min);
	}

	/** Value at normalised {@code fraction}, snapped to {@link #getStep()}. */
	public int valueAtFraction(float fraction) {
		float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
		int raw = this.min + Math.round(clamped * (this.max - this.min));
		int snapped = this.min + ((raw - this.min) / this.step) * this.step;
		return clamp(snapped, this.min, this.max);
	}
}
