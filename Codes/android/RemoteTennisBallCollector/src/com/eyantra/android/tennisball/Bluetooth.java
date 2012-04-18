/**
 * Project Name: Android_FB5
 * Author: 	Jatin Kanzaria.
 * 			K.L.Srinivas.
 * 			Rohan Shah.
 * 			Jagbandhu. 
 * Date:	8/11/2010
 */
/********************************************************************************

   Copyright (c) 2010, ERTS Lab IIT Bombay erts@cse.iitb.ac.in               -*- c -*-
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:

   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the
     distribution.

   * Neither the name of the copyright holders nor the names of
     contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

   * Source code can be used for academic purpose. 
	 For commercial use permission form the author needs to be taken.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE. 

  Software released under Creative Commence cc by-nc-sa licence.
  For legal information refer to: 
  http://creativecommons.org/licenses/by-nc-sa/3.0/legalcode

********************************************************************************/

package com.eyantra.android.tennisball;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.EditText;

/** Class to implement all routines related to bluetooth communication. 
 * 	Task: Initialise bluetooth and establish connection.
 * 		  Send a byte array over the BT channel.
 * 		  Disconnect from the BT device and free the BT channel. 
 */
public class Bluetooth{
	
	final String tag = "Android_FB5";
    
	/** BT related objects. */
	private BluetoothSocket mBluetoothSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private BluetoothDevice mBluetoothDevice = null;
    
    /** UI related objects. */
    private EditText mAddressText = null; 
    private final Activity mactivity;
    
    /** Constructor for the class. Copies the 'activity' object for its use.*/
    public Bluetooth(Activity activity)
    {
    	mactivity = activity;
    }
    
	/** Class for all Bluetooth related functions.
	 *  Task: (1)Acquire a BT socket and connect over that socket.
	 *  	  (2)Establish input and output streams over the socket for data transfer
	 *  Arguments: Null
	 *  Return: True is initialisation was successful, else False. 
	 * @throws Exception
	 */
    public boolean Initialise() throws Exception
	{
		/** Get a handle to the BT hardware. */
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		String add_string;
		/** Get the Address of BT device to be connected with, from the text box on UI. */
		
		add_string = "00:19:A4:02:C6:4E";
		try {
			/** Link the taget BT address to be connected. */
			mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(add_string);
		}catch (IllegalArgumentException e)
		{ 
			/** Exception is thrown if BT address is not valid. Then return false*/
			return false;
		}
		
        Method m;
		m= mBluetoothDevice.getClass().getMethod("createRfcommSocket",new Class[] { int.class });
		mBluetoothSocket = (BluetoothSocket)m.invoke(mBluetoothDevice, Integer.valueOf(1));
        Log.d(tag, "Connecting...");
        
        try {
            /** This is a blocking call and will only return on a successful connection or an exception. */
            mBluetoothSocket.connect();
        } catch (IOException e) {
        	/** If target BT device not found or connection refused then return false. */
            try {
                mBluetoothSocket.close();
            } catch (IOException e2) {
                Log.e(tag, "unable to close() socket during connection failure", e2);
            }
            Log.e(tag,"returning false");
            return false;
        } 

        Log.d(tag, "Connected");
        /** Get input and output stream handles for data transfer. */
		mInputStream = mBluetoothSocket.getInputStream();
		mOutputStream = mBluetoothSocket.getOutputStream();
		return true;
	}
	
	/** Function to send data over BT.
	 * Task: (1)To send the byte array over Bluetooth Channel.
	 * Arguments: An array of bytes to be sent.
	 * Return: Null
	 */
    public void BluetoothSend(byte[] write_buffer)
	{
		try {
         	mOutputStream.write(write_buffer);
         }catch (IOException e){Log.e(tag, "Writing on command error");}
         Log.d(tag, "Writing on command successful");
	}
	
    /** Function to close BT connection.
     * Task: (1)Close input and output streams
     * 		 (2)Close Bluetooth socket.
     * Arguments: Null
     * Return: Null
     */
	public void free_channel()
	{
		try {
            if (mInputStream != null) {
            	mInputStream.close();
            }
            if (mOutputStream != null) {
            	mOutputStream.close();
            }
            if (mBluetoothSocket != null) {
            	mBluetoothSocket.close();
            }
            Log.d(tag, "BT Channel free");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	 
}