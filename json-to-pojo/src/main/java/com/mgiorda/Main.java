package com.mgiorda;

public class Main {

	public static void main(String[] args) {

		JsonToPojo.fromFile("json.txt", "com.mgiorda.test.ApiProxy");
	}

}
