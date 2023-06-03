package com.lenderman.nabu.adapter.extensions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import com.lenderman.nabu.adapter.loader.WebLoader;
import com.lenderman.nabu.adapter.model.file.FileDetails;
import com.lenderman.nabu.adapter.model.file.FileHandle;
import com.lenderman.nabu.adapter.model.file.flags.FileFlagsRetroNet.CopyMoveFlags;
import com.lenderman.nabu.adapter.model.file.flags.FileFlagsRetroNet.FileListFlags;
import com.lenderman.nabu.adapter.model.file.flags.FileFlagsRetroNet.OpenFlags;
import com.lenderman.nabu.adapter.model.file.flags.FileFlagsRetroNet.SeekFlagsRetroNet;
import com.lenderman.nabu.adapter.model.settings.Settings;
import com.lenderman.nabu.adapter.server.ServerInputOutputController;
import com.lenderman.nabu.adapter.utilities.ConversionUtils;
import com.lenderman.nabu.adapter.utilities.WebUtils;

/*
 * Copyright(c) 2023 "RetroTech" Chris Lenderman
 * Copyright(c) 2022 NabuNetwork.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// TODO This class is COMPLETELY UNTESTED and needs EXTENSIVE testing

/**
 * Class to implement all of the NABU File system extensions as defined in
 * https://github.com/DJSures/NABU-LIB/blob/main/NABULIB/RetroNET-FileStore.h
 * only support HTTP(s) and Local files, no FTP
 */
public class FileStoreExtensions implements ServerExtension
{
    /**
     * Class Logger
     */
    private static final Logger logger = Logger
            .getLogger(FileStoreExtensions.class);

    /**
     * Instance of the Server I/O Controller
     */
    private ServerInputOutputController sioc;

    /**
     * Program settings
     */
    private Settings settings;

    /**
     * We keep track of the file handles opened by NABU with a quick /
     * dictionary
     */
    private ConcurrentHashMap<Byte, FileHandle> fileHandles;

    /**
     * When fileList() is called, we create a list of the files which can then
     * be returned one at a time with a call to FileListItem()
     */
    private List<FileDetails> fileDetails;

    /**
     * Constructor
     */
    public FileStoreExtensions(ServerInputOutputController sioc,
            Settings settings)
    {
        this.sioc = sioc;
        this.settings = settings;
        this.initialize();
    }

    /**
     * This extension implements several new op codes - This function maps /
     * those codes to the appropriate function call.
     *
     * @param opCode OP code to process
     * @return true if we acted on this opCode, otherwise false.</returns>
     */
    public boolean tryProcessCommand(int opCode) throws Exception
    {
        switch (opCode)
        {
        case 0xA3:
            this.fileOpen();
            return true;
        case 0xA4:
            this.fileHandleSize();
            return true;
        case 0xA5:
            this.fileHandleRead();
            return true;
        case 0xA7:
            this.fileHandleClose();
            return true;
        case 0xA8:
            this.fileSize();
            return true;
        case 0xA9:
            this.fileHandleAppend();
            return true;
        case 0xAA:
            this.fileHandleInsert();
            return true;
        case 0xAB:
            this.fileHandleDeleteRange();
            return true;
        case 0xAC:
            this.fileHandleReplace();
            return true;
        case 0xAD:
            this.fileDelete();
            return true;
        case 0xAE:
            this.fileHandleCopy();
            return true;
        case 0xAF:
            this.fileHandleMove();
            return true;
        case 0xB0:
            this.fileHandleEmptyFile();
            return true;
        case 0xB1:
            this.fileList();
            return true;
        case 0xB2:
            this.fileListItem();
            return true;
        case 0xB3:
            this.fileDetails();
            return true;
        case 0xB4:
            this.fileHandleDetails();
            return true;
        case 0xB5:
            this.fileHandleReadSeq();
            return true;
        case 0xB6:
            this.fileHandleSeek();
            return true;
        }

        // Op code not serviced by this extension
        return false;
    }

    /**
     * Reset this extension. If the Nabu starts over loading segment 0 and
     */
    public void reset()
    {
        logger.debug("Resetting FileStoreExtension");
        this.initialize();
    }

    /**
     * Initialize the extension - setup the member variables.
     */
    private void initialize()
    {
        this.fileHandles = new ConcurrentHashMap<Byte, FileHandle>();
        this.fileDetails = new ArrayList<FileDetails>();
    }

    /**
     * fileOpen
     */
    private void fileOpen() throws Exception
    {
        // First byte is the string length
        int length = this.sioc.getIs().readByte();

        String fileName = this.sioc.getIs().readString(length);
        if (fileName.toLowerCase().startsWith("http"))
        {
            // Download this file from wherever it is located to the current
            // directory.
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1,
                    fileName.length());

            // not a valid url, send back
            if (!WebUtils.validateUri(fileName))
            {
                this.sioc.getOs().writeBytes(0xFF);
                return;
            }

            File fullPathAndFilename = new File(
                    this.settings.getWorkingDirectory() + File.separator
                            + fileName);
            if (!fullPathAndFilename.exists())
            {
                byte[] data;

                WebLoader webLoader = new WebLoader();
                data = webLoader.tryGetData(fileName);
                FileOutputStream outputStream = new FileOutputStream(
                        fullPathAndFilename);
                outputStream.write(data);
                outputStream.close();
            }
        }

        fileName = this.sanitizeFilename(fileName);

        // Read the flags
        int fileFlags = this.sioc.getIs().readShort();

        // Read the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // If this handle is the max value, or that this handle is already in
        // use, find the first unused handle
        if (fileHandle == ConversionUtils.MAX_BYTE_VALUE || this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle)) != null)
        {
            // find the first unused file handle
            for (int key = 0; key <= ConversionUtils.MAX_BYTE_VALUE; key++)
            {
                if (!fileHandles.containsKey(ConversionUtils.byteVal(key)))
                {
                    fileHandle = key;
                    break;
                }
            }
        }

        FileHandle nabuFileHandle = new FileHandle(
                this.settings.getWorkingDirectory(), fileName, fileFlags,
                fileHandle);

        // Mark this handle as in use.
        this.fileHandles.put(ConversionUtils.byteVal(fileHandle),
                nabuFileHandle);

        // Let the NABU know what the real file handle is
        this.sioc.getOs().writeBytes(fileHandle);
    }

    /**
     * File Handle Size
     */
    private void fileHandleSize() throws Exception
    {
        // first byte, the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // Retrieve this file handle from the file handle list.
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null)
        {
            this.sioc.getOs()
                    .writeInt(this.fileSize(nabuFileHandle.getFullFilename()));
        }
        else
        {
            logger.error(
                    "Requested file handle " + String.format("%06x", fileHandle)
                            + " but it was not found, returning -1");
            this.sioc.getOs().writeInt(-1L);
        }
    }

    /**
     * File Handle Read
     */
    private void fileHandleRead() throws Exception
    {
        // first byte, the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // the offset
        long offset = this.sioc.getIs().readInt();

        // the length
        int length = this.sioc.getIs().readShort();

        // Retrieve this file handle from the file handle list.
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null)
        {
            RandomAccessFile seeker = new RandomAccessFile(
                    nabuFileHandle.getFullFilename(), "r");
            seeker.seek(offset);
            byte[] data = new byte[length];
            seeker.read(data);
            this.sioc.getOs().writeShort(data.length);
            this.sioc.getOs().writeBytes(data);
        }
        else
        {
            logger.error("Requested file handle to read: "
                    + String.format("%06x", fileHandle)
                    + ", but it was not found");

            // sending back 0, this tells the NABU there is no data to read
            sioc.getOs().writeShort(0);
        }
    }

    /**
     * File Handle Close
     */
    private void fileHandleClose() throws Exception
    {
        // first byte, the file handle
        int fileHandle = this.sioc.getIs().readByte();
        this.fileHandles.remove(ConversionUtils.byteVal(fileHandle));
    }

    /**
     * File Size
     */
    private void fileSize() throws Exception
    {
        // First byte is the string length
        int length = this.sioc.getIs().readByte();

        // read filename
        String fileName = this.sioc.getIs().readString(length);
        this.sioc.getOs()
                .writeInt(this.fileSize(this.settings.getWorkingDirectory()
                        + File.separator + sanitizeFilename(fileName)));
    }

    /**
     * File Handle Append
     */
    private void fileHandleAppend() throws Exception
    {
        // first byte, the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // now the length of the data
        int length = this.sioc.getIs().readShort();

        // read the data into an array
        byte[] data = this.sioc.getIs().readBytes(length);

        // ok, get the specified file handle.
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null && nabuFileHandle.getFlagsAsOpenFlags()
                .contains(OpenFlags.ReadWrite))
        {
            OutputStream os = new FileOutputStream(
                    nabuFileHandle.getFullFilename(), true);
            os.write(data);
            os.close();
        }
        else
        {
            logger.error("Requested file Append on "
                    + String.format("%06x", fileHandle)
                    + ", but it was not found");
        }
    }

    /**
     * File Handle Insert
     */
    private void fileHandleInsert() throws Exception
    {
        // first byte, the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // read the file index
        long index = this.sioc.getIs().readInt();

        // read the data length
        int length = this.sioc.getIs().readShort();

        // Read the data from the buffer
        byte[] data = this.sioc.getIs().readBytes(length);

        List<Byte> datalist = new ArrayList<Byte>();
        for (byte b : data)
        {
            datalist.add(b);
        }

        // Get the file handle
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null && ((nabuFileHandle.getFlagsAsOpenFlags()
                .contains(OpenFlags.ReadWrite))))
        {

            RandomAccessFile seeker = new RandomAccessFile(
                    nabuFileHandle.getFullFilename(), "r");
            byte[] bytelist = new byte[(int) seeker.length()];
            seeker.read(bytelist);

            List<Byte> list = new ArrayList<Byte>();
            for (byte b : bytelist)
            {
                list.add(b);
            }
            list.addAll((int) index, datalist);
            Byte[] bytes = list.toArray(new Byte[list.size()]);
            byte[] bytes2 = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++)
            {
                bytes2[i] = bytes[i];
            }
            FileOutputStream outputStream = new FileOutputStream(
                    nabuFileHandle.getFullFilename());
            outputStream.write(bytes2);
            outputStream.close();
        }
        else
        {
            logger.error("Requested handle insert on "
                    + String.format("%06x", fileHandle)
                    + ", but it was not found");
        }
    }

    /**
     * File Handle Delete Range
     */
    private void fileHandleDeleteRange() throws Exception
    {
        // first byte, file handle
        int fileHandle = this.sioc.getIs().readByte();

        // read the file offset
        long index = this.sioc.getIs().readInt();

        // read the length
        int length = this.sioc.getIs().readShort();

        // Get the file handle
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null && ((nabuFileHandle.getFlagsAsOpenFlags()
                .contains(OpenFlags.ReadWrite))))
        {
            RandomAccessFile seeker = new RandomAccessFile(
                    nabuFileHandle.getFullFilename(), "r");
            byte[] bytelist = new byte[(int) seeker.length()];
            seeker.read(bytelist);

            List<Byte> list = new ArrayList<Byte>();
            for (byte b : bytelist)
            {
                list.add(b);
            }
            ArrayList<Byte> arraylist = new ArrayList<Byte>(list);
            arraylist.subList((int) index, (int) index + length).clear();
            Byte[] bytes = arraylist.toArray(new Byte[arraylist.size()]);
            byte[] bytes2 = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++)
            {
                bytes2[i] = bytes[i];
            }

            FileOutputStream outputStream = new FileOutputStream(
                    nabuFileHandle.getFullFilename());
            outputStream.write(bytes2);
            outputStream.close();
        }
        else
        {
            logger.error(
                    "Requested file handle " + String.format("%06x", fileHandle)
                            + ", but it was not found");
        }
    }

    /**
     * File Handle Replace
     */
    private void fileHandleReplace() throws Exception
    {
        // Get the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // Get the file offset
        long index = this.sioc.getIs().readInt();

        // get the data length
        int length = this.sioc.getIs().readShort();

        // Get the data
        byte[] data = this.sioc.getIs().readBytes(length);

        // Get the file handle
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null && nabuFileHandle.getFlagsAsOpenFlags()
                .contains(OpenFlags.ReadWrite))
        {

            RandomAccessFile seeker = new RandomAccessFile(
                    nabuFileHandle.getFullFilename(), "r");
            byte[] bytelist = new byte[(int) seeker.length()];
            seeker.read(bytelist);

            for (int i = 0; i < length; i++)
            {
                bytelist[(int) (i + index)] = data[i];
            }
            FileOutputStream outputStream = new FileOutputStream(
                    nabuFileHandle.getFullFilename());
            outputStream.write(bytelist);
            outputStream.close();
        }
        else
        {
            logger.error(
                    "Requested file handle " + String.format("%06x", fileHandle)
                            + ", but it was not found");
        }
    }

    /**
     * File Delete
     */
    private void fileDelete() throws Exception
    {
        // Read the filename length
        int length = this.sioc.getIs().readByte();

        // read the filename
        String fileName = this.sioc.getIs().readString(length);
        File path = new File(this.settings.getWorkingDirectory()
                + File.separator + sanitizeFilename(fileName));

        if (path.exists())
        {
            path.delete();
        }

        // Must be a better way to do this - Find all instances of this file in
        // the file handles and close them.
        for (int i = 0; i <= ConversionUtils.MAX_BYTE_VALUE; i++)
        {
            if (this.fileHandles.get(ConversionUtils.byteVal(i)) != null
                    && this.fileHandles.get(ConversionUtils.byteVal(i))
                            .getFullFilename().toString()
                            .equalsIgnoreCase(fileName))
            {
                // clear out this file handle
                this.fileHandles.remove(ConversionUtils.byteVal(i));
            }
        }
    }

    /**
     * File Handle Copy
     */
    private void fileHandleCopy() throws Exception
    {
        // read the source filename
        int length = this.sioc.getIs().readByte();
        String sourceFilename = this.sioc.getIs().readString(length);

        // read the destination filename
        length = this.sioc.getIs().readByte();
        String destinationFilename = this.sioc.getIs().readString(length);

        // read the copy move flag
        List<CopyMoveFlags> flags = CopyMoveFlags
                .parse(this.sioc.getIs().readByte());

        File source = new File(this.settings.getWorkingDirectory()
                + File.separator + sanitizeFilename(sourceFilename));
        File destination = new File(this.settings.getWorkingDirectory()
                + File.separator + sanitizeFilename(destinationFilename));

        if (!destination.exists() || (destination.exists())
                && (flags.contains(CopyMoveFlags.YesReplace)))
        {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(source));
            OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(destination));

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0)
            {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
            in.close();
            out.close();
        }
    }

    /**
     * File Handle Move
     */
    private void fileHandleMove() throws Exception
    {
        // read the source filename
        int length = this.sioc.getIs().readByte();
        String sourceFilename = this.sioc.getIs().readString(length);

        // read the destination filename
        length = this.sioc.getIs().readByte();
        String destinationFilename = this.sioc.getIs().readString(length);

        // read the copy move flag
        List<CopyMoveFlags> flags = CopyMoveFlags
                .parse(this.sioc.getIs().readByte());

        File source = new File(this.settings.getWorkingDirectory()
                + File.separator + sanitizeFilename(sourceFilename));
        File destination = new File(this.settings.getWorkingDirectory()
                + File.separator + sanitizeFilename(destinationFilename));

        if (!destination.exists() || (destination.exists()
                && (flags.contains(CopyMoveFlags.YesReplace))))
        {
            source.renameTo(destination);
        }
    }

    /**
     * File Handle Empty File
     */
    private void fileHandleEmptyFile() throws Exception
    {
        // Read in the file handle.
        int fileHandle = this.sioc.getIs().readByte();

        // Get the file handle
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null)
        {
            (new File(nabuFileHandle.getFullFilename())).createNewFile();
        }
        else
        {
            logger.error(
                    "Requested file handle " + String.format("%06x", fileHandle)
                            + ", but it was not found");
        }
    }

    /**
     * File List (basically, do a DIR based on the search pattern, store the
     * results for user later)
     */
    private void fileList() throws Exception
    {
        // Read the path length
        int length = this.sioc.getIs().readByte();

        // Get the path
        String path = this.sioc.getIs().readString(length);

        // read the search pattern length
        length = this.sioc.getIs().readByte();

        // read the search pattern
        String searchPattern = this.sioc.getIs().readString(length);

        // Get the flags
        List<FileListFlags> flags = FileListFlags
                .parse(this.sioc.getIs().readByte());

        boolean includeDirectories = flags
                .contains(FileListFlags.IncludeDirectories);
        boolean includeFiles = flags.contains(FileListFlags.IncludeFiles);

        this.fileDetails.clear();

        if (includeDirectories)
        {
            FileFilter fileFilter = new WildcardFileFilter(searchPattern);
            File dir = new File(this.settings.getWorkingDirectory()
                    + File.separator + path);
            File[] files = dir.listFiles(fileFilter);

            for (File file : files)
            {
                if (file.isDirectory() && includeDirectories)
                {
                    this.fileDetails
                            .add(new FileDetails(file.getAbsolutePath()));
                }
                if (!file.isDirectory() && includeFiles)
                {
                    this.fileDetails
                            .add(new FileDetails(file.getAbsolutePath()));
                }
            }
        }

        sioc.getOs().writeShort(fileDetails.size());
    }

    /**
     * File List Item
     */
    private void fileListItem() throws Exception
    {
        // read in the index number for the file list cache.
        int num = sioc.getIs().readShort();
        sioc.getOs().writeBytes(this.fileDetails.get(num).getFileDetails());
    }

    /**
     * File Details (This is with a file name)
     */
    private void fileDetails() throws Exception
    {
        // read in the filename
        int length = sioc.getIs().readByte();
        String filename = sioc.getIs().readString(length);
        this.fileDetails(this.settings.getWorkingDirectory() + File.separator
                + sanitizeFilename(filename));
    }

    /**
     * File Handle Details (This is with a file handle)
     */
    private void fileHandleDetails() throws Exception
    {
        // Read the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // Retrieve this file handle from the file handle list.
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        // if the file handle is present, what the heck?
        if (nabuFileHandle != null)
        {
            this.fileDetails(nabuFileHandle.getFullFilename());
        }
        else
        {
            logger.error("Requested file handle for FileHandleDetails: "
                    + String.format("%06x", fileHandle)
                    + ", but it was not found");
        }
    }

    /**
     * Return a FileDetails based on the filename
     *
     * @param filePath
     */
    private void fileDetails(String filePath) throws Exception
    {
        FileDetails fileDetails;

        File file = new File(filePath);
        if (file.exists())
        {
            fileDetails = new FileDetails(filePath);
        }
        else
        {
            // fake it
            fileDetails = new FileDetails(Calendar.getInstance(),
                    Calendar.getInstance(), "\\", -2);
        }

        this.sioc.getOs().writeBytes(fileDetails.getFileDetails());
    }

    /**
     * File Handle Sequential Read
     */
    private void fileHandleReadSeq() throws Exception
    {
        // Read the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // Read the number of bytes to read
        int length = this.sioc.getIs().readShort();

        // Retrieve this file handle from the file handle list.
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        // if the file handle is present, what the heck?
        if (nabuFileHandle != null)
        {
            RandomAccessFile file = new RandomAccessFile(
                    nabuFileHandle.getFullFilename(), "r");
            byte[] data = new byte[length];
            file.read(data, (int) nabuFileHandle.getIndex(), length);
            nabuFileHandle.setIndex(nabuFileHandle.getIndex() + length);

            // write how much data we got
            this.sioc.getOs().writeShort(data.length);

            // write the data
            this.sioc.getOs().writeBytes(data);
        }
        else
        {
            logger.error("Requested file handle for FileHandleReadSeq: "
                    + String.format("%06x", fileHandle)
                    + ", but it was not found");
        }
    }

    /**
     * File Handle Sequential Read
     */
    private void fileHandleSeek() throws Exception
    {
        // read the file handle
        int fileHandle = this.sioc.getIs().readByte();

        // read the offset
        long offset = this.sioc.getIs().readInt();

        // read the seek options
        int seekOption = this.sioc.getIs().readByte();
        List<SeekFlagsRetroNet> seekFlags = SeekFlagsRetroNet.parse(seekOption);

        // Retrieve this file handle from the file handle list.
        FileHandle nabuFileHandle = this.fileHandles
                .get(ConversionUtils.byteVal(fileHandle));

        if (nabuFileHandle != null)
        {
            long fileSize = (new File(nabuFileHandle.getFullFilename()))
                    .length();

            if (seekFlags.contains(SeekFlagsRetroNet.SET))
            {
                // Seek from the start of the file
                nabuFileHandle.setIndex(offset);
            }
            else if (seekFlags.contains(SeekFlagsRetroNet.CUR))
            {
                // Seek from the current position in the file.
                nabuFileHandle.setIndex(nabuFileHandle.getIndex() + offset);
            }
            else
            {
                // Last option is from the end of the file.
                nabuFileHandle.setIndex(fileSize - offset);
            }

            if (nabuFileHandle.getIndex() < 0)
            {
                nabuFileHandle.setIndex(0);
            }
            else if (nabuFileHandle.getIndex() > fileSize)
            {
                nabuFileHandle.setIndex(fileSize);
            }

            sioc.getOs().writeInt(nabuFileHandle.getIndex());
        }
        else
        {
            logger.error("Requested file handle for FileHandleSeek: "
                    + String.format("%06x", fileHandle)
                    + ", but it was not found");
        }
    }

    /**
     * File Size
     *
     * @param fileName
     * @return file size
     */
    private long fileSize(String fileName) throws Exception
    {
        File file = new File(fileName);
        if (file.exists())
        {
            return file.length();
        }
        else
        {
            logger.error(
                    "Unable to find filename {}, returning -1: " + fileName);
            return -1;
        }
    }

    /**
     * Sanitize Filename
     *
     * @param path
     * @return String
     */
    private String sanitizeFilename(String path)
    {
        int extensionIndex = path.lastIndexOf('.');
        String extension = null;
        if (extensionIndex > 0)
        {
            extension = path.substring(extensionIndex + 1);
        }

        if (extension == null || !Settings.allowedExtensions
                .contains(extension.toLowerCase()))
        {
            logger.error(
                    "NABU requested a file extension which is not allowed: "
                            + path);
        }

        return path;
    }
}
