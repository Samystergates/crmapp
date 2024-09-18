
package com.web.appts.DTO;

public class WheelColorDto {
	private long id;
	private String colorName;
	private String codeVert;
	private String codePoeder;
	private int red;
	private int green;
	private int blue;

	public WheelColorDto() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getColorName() {
		return this.colorName;
	}

	public void setColorName(String colorName) {
		this.colorName = colorName;
	}

	public String getCodeVert() {
		return this.codeVert;
	}

	public void setCodeVert(String codeVert) {
		this.codeVert = codeVert;
	}

	public String getCodePoeder() {
		return this.codePoeder;
	}

	public void setCodePoeder(String codePoeder) {
		this.codePoeder = codePoeder;
	}

	public int getRed() {
		return this.red;
	}

	public void setRed(int red) {
		this.red = red;
	}

	public int getGreen() {
		return this.green;
	}

	public void setGreen(int green) {
		this.green = green;
	}

	public int getBlue() {
		return this.blue;
	}

	public void setBlue(int blue) {
		this.blue = blue;
	}
}
