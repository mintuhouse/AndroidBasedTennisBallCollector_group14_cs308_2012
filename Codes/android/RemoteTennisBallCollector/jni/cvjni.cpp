/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "cv.h"
#include "cxcore.h"
#include "bmpfmt.h"
#include <stdio.h>

#define ANDROID_LOG_VERBOSE ANDROID_LOG_DEBUG
#define LOG_TAG "CVJNI"
#define LOGV(...) __android_log_print(ANDROID_LOG_SILENT, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#ifdef __cplusplus
extern "C" {
#endif
IplImage* pImage = NULL;
IplImage* loadPixels(int* pixels, int width, int height);
IplImage* getIplImageFromIntArray(JNIEnv* env, jintArray array_data,
		jint width, jint height);

JNIEXPORT void JNICALL Java_com_eyantra_android_tennisball_OpenCV_extractSURFFeature(
		JNIEnv* env, jobject thiz) {
	IplImage *pWorkImage=cvCreateImage(cvGetSize(pImage),IPL_DEPTH_8U,1);
	cvCvtColor(pImage,pWorkImage,CV_BGR2GRAY);
	CvMemStorage* storage = cvCreateMemStorage(0);
	CvSeq *imageKeypoints = 0, *imageDescriptors = 0;
	CvSURFParams params = cvSURFParams(2000, 0);
	cvExtractSURF( pWorkImage, 0, &imageKeypoints, &imageDescriptors, storage, params );
	// show features
	for( int i = 0; i < imageKeypoints->total; i++ )
	{
		CvSURFPoint* r = (CvSURFPoint*)cvGetSeqElem( imageKeypoints, i );
		CvPoint center;
		int radius;
		center.x = cvRound(r->pt.x);
		center.y = cvRound(r->pt.y);
		radius = cvRound(r->size*1.2/9.*2);
		cvCircle( pImage, center, radius, CV_RGB(255,0,0), 1, CV_AA, 0 );
	}
	cvReleaseImage(&pWorkImage);
	cvReleaseMemStorage(&storage);
}

JNIEXPORT jintArray JNICALL Java_com_eyantra_android_tennisball_OpenCV_locateBall(
		JNIEnv* env, jobject thiz) {
	jintArray result = env->NewIntArray(6);

	IplImage *pWorkImage=cvCreateImage(cvGetSize(pImage),IPL_DEPTH_8U,1);
    int width 	= pImage->width;
    int height 	= pImage->height;
    int step 	= pImage->widthStep;
    int channels 	= pImage->nChannels;
    int wstep 	= pWorkImage->widthStep;
    int wchannels	= pWorkImage->nChannels;

    uchar* data 	= (uchar*)pImage->imageData;
    uchar* wdata 	= (uchar*)pWorkImage->imageData;

    int x1=-1, x2=-1, y1=-1, y2=-1;
    int circleDetected=0;

    /*
     * Converting the Captured Image into binary format.
     * TODO: Explain algo used
     */
    for(int i = 0;i < height;i++) {
		for(int j = 0;j < width;j++) {
			if( (data[i*step+j*channels+2] > 50+data[i*step+j*channels])
					&& data[i*step+j*channels+2] >= 225
					&& (data[i*step+j*channels+2] > 50+data[i*step+j*channels+1]) )
			{
				   wdata[i*wstep+j*wchannels] = 0;
			}
			else wdata[i*wstep+j*wchannels] = 255;
		}
	}

	for(int i=0;i<width;i++){
		int count=0;
		for(int j=0;j<height;j++){
			if(wdata[j*wstep+i*wchannels]==0){
				count++;
			}
		}
		if(count>10){
			if(x1 == -1) x1 = i;
			else x2 = i;
		}
	}

	for(int i=0;i<height;i++){
		int count=0;
		for(int j=0;j<width;j++){
			if(wdata[j*wchannels+i*wstep]==0){
				count++;
			}
		}
		if(count>10){
			if(y1 == -1) y1 = i;
			else y2 = i;
		}
	}

	if(x1!=-1&&y1!=-1) circleDetected=1;

	jint A[6];
	A[0] = circleDetected;	// isCircleDetected
	A[1] = (x1+x2)/2;		// centre - x
	A[2] = (y1+y2)/2;		// centre - y
	A[3] = (x2-x1+y2-y1)/4;// radius
	A[4] = width;			// Image Width
	A[5] = height;			// Image Height

	// show features
	cvCircle( pImage, cvPoint(A[1],A[2]), A[3], CV_RGB(255,255,0), 3, 8, 0 );

	cvReleaseImage(&pWorkImage);

	env->SetIntArrayRegion(result, 0, 6, A);
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_eyantra_android_tennisball_OpenCV_setSourceImage(
		JNIEnv* env, jobject thiz, jintArray photo_data, jint width,
		jint height) {
	if (pImage != NULL) {
		cvReleaseImage(&pImage);
		pImage = NULL;
	}
	pImage = getIplImageFromIntArray(env, photo_data, width, height);
	if (pImage == NULL) {
		return 0;
	}
	LOGI("Load Image Done.");
	return 1;
}
JNIEXPORT jbooleanArray JNICALL Java_com_eyantra_android_tennisball_OpenCV_getSourceImage(
		JNIEnv* env, jobject thiz) {
	if (pImage == NULL) {
		LOGE("No source image.");
		return 0;
	}
	cvFlip(pImage);
	int width = pImage->width;
	int height = pImage->height;
	int rowStep = pImage->widthStep;
	int headerSize = 54;
	int imageSize = rowStep * height;
	int fileSize = headerSize + imageSize;
	unsigned char* image = new unsigned char[fileSize];
	struct bmpfile_header* fileHeader = (struct bmpfile_header*) (image);
	fileHeader->magic[0] = 'B';
	fileHeader->magic[1] = 'M';
	fileHeader->filesz = fileSize;
	fileHeader->creator1 = 0;
	fileHeader->creator2 = 0;
	fileHeader->bmp_offset = 54;
	struct bmp_dib_v3_header_t* imageHeader =
			(struct bmp_dib_v3_header_t*) (image + 14);
	imageHeader->header_sz = 40;
	imageHeader->width = width;
	imageHeader->height = height;
	imageHeader->nplanes = 1;
	imageHeader->bitspp = 24;
	imageHeader->compress_type = 0;
	imageHeader->bmp_bytesz = imageSize;
	imageHeader->hres = 0;
	imageHeader->vres = 0;
	imageHeader->ncolors = 0;
	imageHeader->nimpcolors = 0;
	memcpy(image + 54, pImage->imageData, imageSize);
	jbooleanArray bytes = env->NewBooleanArray(fileSize);
	if (bytes == 0) {
		LOGE("Error in creating the image.");
		delete[] image;
		return 0;
	}
	env->SetBooleanArrayRegion(bytes, 0, fileSize, (jboolean*) image);
	delete[] image;
	return bytes;
}

IplImage* loadPixels(int* pixels, int width, int height) {
	int x, y;
	IplImage *img = cvCreateImage(cvSize(width, height), IPL_DEPTH_8U, 3);
	unsigned char* base = (unsigned char*) (img->imageData);
	unsigned char* ptr;
	for (y = 0; y < height; y++) {
		ptr = base + y * img->widthStep;
		for (x = 0; x < width; x++) {
			// blue
			ptr[3 * x] = pixels[x + y * width] & 0xFF;
			// green
			ptr[3 * x + 1] = pixels[x + y * width] >> 8 & 0xFF;
			// blue
			ptr[3 * x + 2] = pixels[x + y * width] >> 16 & 0xFF;
		}
	}
	return img;
}
IplImage* getIplImageFromIntArray(JNIEnv* env, jintArray array_data,
		jint width, jint height) {
	int *pixels = env->GetIntArrayElements(array_data, 0);
	if (pixels == 0) {
		LOGE("Error getting int array of pixels.");
		return 0;
	}
	IplImage *image = loadPixels(pixels, width, height);
	env->ReleaseIntArrayElements(array_data, pixels, 0);
	if (image == 0) {
		LOGE("Error loading pixel array.");
		return 0;
	}
	return image;
}

#ifdef __cplusplus
}
#endif

