package com.vicr123.client.iom

import java.io.BufferedReader

class IOMHttpException(val responseCode: Int, val bufferedReader: BufferedReader) : Exception() {

}