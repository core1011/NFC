#include <jni.h>
#include <string>
#include <android/log.h>
#include<fcntl.h>
#include<pthread.h>
#include<stdio.h>      /*标准输入输出定义*/
#include<stdlib.h>     /*标准函数库定义*/
#include<unistd.h>     /*Unix 标准函数定义*/
#include<sys/types.h>
#include<sys/stat.h>
#include<termios.h>    /*PPSIX 终端控制定义*/
#include<errno.h>      /*错误号定义*/
#include <wchar.h>





#define  LOG_TAG    "md-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

JNIEXPORT jobject JNICALL
Java_com_ll_clothespad_serialport_SerialPort_open(JNIEnv *env, jclass type, jstring path_,
                                                     jint baudrate, jint flags) {
    const char *path = env->GetStringUTFChars(path_, 0);
    // TODO
    env->ReleaseStringUTFChars(path_, path);
    return 0;
}

int setserialport(int fd,int speed,int flow_ctrl,int databits,int stopbits,char parity )
{

    int   i;
    int   status;
    int   speed_arr[] = { B115200, B19200, B9600, B4800, B2400, B1200, B300};
    int   name_arr[] = {115200,  19200,  9600,  4800,  2400,  1200,  300};

    struct termios options;
    memset(&options,0x00, sizeof(struct termios));
    /*tcgetattr(fd,&options)得到与fd指向对象的相关参数，并将它们保存于options,该函数还可以测试配置是否正确，该串口是否可用等。若调用成功，函数返回值为0，若调用失败，函数返回值为1.

    */
    /*if( tcgetattr( fd,&options)  !=  0)
    {
        perror("SetupSerial 1");
        return(-1);
    }
     */

    //设置串口输入波特率和输出波特率
    for ( i= 0;  i < sizeof(speed_arr) / sizeof(int);  i++)
    {
        if(speed == name_arr[i])
        {
            cfsetispeed(&options, speed_arr[i]);
            cfsetospeed(&options, speed_arr[i]);
        }
    }

    //修改控制模式，保证程序不会占用串口
    options.c_cflag |= CLOCAL;
    //修改控制模式，使得能够从串口中读取输入数据
    options.c_cflag |= CREAD;

/*    //设置数据流控制
    switch(flow_ctrl)
    {

        case 0 ://不使用流控制
            options.c_cflag &= ~CRTSCTS;
            break;

        case 1 ://使用硬件流控制
            options.c_cflag |= CRTSCTS;
            break;
        case 2 ://使用软件流控制
            options.c_cflag |= IXON | IXOFF | IXANY;
            break;
    }*/
    //设置数据位
    //屏蔽其他标志位
    options.c_cflag &= ~CSIZE;
    switch (databits)
    {
        case 5:
            options.c_cflag |= CS5;
            break;
        case 6:
            options.c_cflag |= CS6;
            break;
        case 7:
            options.c_cflag |= CS7;
            break;
        case 8:
            options.c_cflag |= CS8;
            break;
        default:
            fprintf(stderr,"Unsupported data size\n");
            return (-1);
    }
    //设置校验位
    switch (parity)
    {
        case 'n':
        case 'N': //无奇偶校验位。
            options.c_cflag &= ~PARENB;
            options.c_iflag &= ~INPCK;
            break;
        case 'o':
        case 'O'://设置为奇校验
            options.c_cflag |= (PARODD | PARENB);
            options.c_iflag |= INPCK;
            break;
        case 'e':
        case 'E'://设置为偶校验
            options.c_cflag |= PARENB;
            options.c_cflag &= ~PARODD;
            options.c_iflag |= INPCK;
            break;
        case 's':
        case 'S': //设置为空格
            options.c_cflag &= ~PARENB;
            options.c_cflag &= ~CSTOPB;
            break;
        default:
            fprintf(stderr,"Unsupported parity\n");
            return (-1);
    }
    // 设置停止位
    switch (stopbits)
    {
        case 1:
            options.c_cflag &= ~CSTOPB; break;
        case 2:
            options.c_cflag |= CSTOPB; break;
        default:
            fprintf(stderr,"Unsupported stop bits\n");
            return (-1);
    }

    //修改输出模式，原始数据输出
   // options.c_oflag &= ~OPOST;

    //options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
//options.c_lflag &= ~(ISIG | ICANON);

    //设置等待时间和最小接收字符
   // options.c_cc[VTIME] = 1; /* 读取一个字符等待1*(1/10)s */
   // options.c_cc[VMIN] = 1; /* 读取字符的最少个数为1 */

    //如果发生数据溢出，接收数据，但是不再读取 刷新收到的数据但是不读
    tcflush(fd,TCIFLUSH);

    //激活配置 (将修改后的termios数据设置到串口中）
    if (tcsetattr(fd,TCSANOW,&options) != 0)
    {
        perror("com set error!\n");
        return (-1);
    }
    return (0);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_ll_nfcdemo2_SerialTask_SerialPortClose(JNIEnv *env, jobject instance,jint fd)
{
    close(fd);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_ll_nfcdemo2_SerialTask_SerialPortOpen(JNIEnv *env, jobject instance)
{
    // TODO

    int fd = 0;
    fd = open("/dev/ttyS3",O_RDWR);
    std::string str;
    LOGI("2019-04-25 serial_Init()-> fd = %d \n",fd);
    if(fd>=0)
    {
        str ="open /dev/ttyS3 ok";

        if(fcntl(fd,F_SETFL,O_NONBLOCK) < 0)
        {
            LOGE("fcntl failed\n");
        }
        else
        {
            LOGI("serial fcntl=%d set fd noblock sucess\n",fcntl(fd,F_SETFL,O_NONBLOCK));
        }
       int value = setserialport(fd,19200,0,8,1,'N');
       if(value>=0)
        {
            LOGI("set serial success!\n");
        }
    }
    return fd;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_ll_nfcdemo2_SerialTask_RecvSeralPort(JNIEnv *env, jobject instance,jint fd,jbyteArray buf) {

    // TODO
    jchar  i = 0;
    jbyte rcv_buf[256]={0};
/*
    jbyte *p = (*env).GetByteArrayElements(buf, NULL);
    if (p != NULL) {
        for (i = 0; i < 255; i++)
            p[i] = '1';

        env->ReleaseByteArrayElements(buf, p, NULL);
    }
    return i;
*/
    int len=0,fs_sel = 0;
    fd_set fs_read;

    struct timeval time;

    FD_ZERO(&fs_read);
    FD_SET(fd,&fs_read);

    time.tv_sec = 0;
    time.tv_usec = 1000*10;

    //使用select实现串口的多路通信
    fs_sel = select(fd+1,&fs_read,NULL,NULL,&time);
    if(fs_sel >0)
    {
        if (FD_ISSET(fd,&fs_read))
        {
            len = read(fd,rcv_buf,255);
            len = len %256;
            if(len>0)
            {
                jbyte *p = (*env).GetByteArrayElements(buf, NULL);
                if (p != NULL)
                {
                    for (i = 0; i < len; i++)
                    {
                        p[i] = rcv_buf[i];
                        LOGI("%02X ", rcv_buf[i]);
                    }
                    LOGI("serial len=%d ", len);
                    env->ReleaseByteArrayElements(buf, p, NULL);
                }
            }
        }
        return len;
    }
    else if(fs_sel<0)
    {
        //LOGE("select(error  fs_sel=%d\n",fs_sel);
        return 0;
    }
    else if(fs_sel == 0) {
        //LOGE("select  timeout fs_sel=%d\n", fs_sel);
        return  0;
    }
}











