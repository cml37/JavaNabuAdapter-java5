package com.lenderman.nabu.adapter.utilities;

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

import java.util.List;

public class CRC
{
    /**
     * Cycle CRC calculation support table
     */
    private static final int[] cycleCrcTable =
    { 0, 4129, 8258, 12387, 16516, 20645, 24774, 28903, 33032, 37161, 41290,
            45419, 49548, 53677, 57806, 61935, 4657, 528, 12915, 8786, 21173,
            17044, 29431, 25302, 37689, 33560, 45947, 41818, 54205, 50076,
            62463, 58334, 9314, 13379, 1056, 5121, 25830, 29895, 17572, 21637,
            42346, 46411, 34088, 38153, 58862, 62927, 50604, 54669, 13907, 9842,
            5649, 1584, 30423, 26358, 22165, 18100, 46939, 42874, 38681, 34616,
            63455, 59390, 55197, 51132, 18628, 22757, 26758, 30887, 2112, 6241,
            10242, 14371, 51660, 55789, 59790, 63919, 35144, 39273, 43274,
            47403, 23285, 19156, 31415, 27286, 6769, 2640, 14899, 10770, 56317,
            52188, 64447, 60318, 39801, 35672, 47931, 43802, 27814, 31879,
            19684, 23749, 11298, 15363, 3168, 7233, 60846, 64911, 52716, 56781,
            44330, 48395, 36200, 40265, 32407, 28342, 24277, 20212, 15891,
            11826, 7761, 3696, 65439, 61374, 57309, 53244, 48923, 44858, 40793,
            36728, 37256, 33193, 45514, 41451, 53516, 49453, 61774, 57711, 4224,
            161, 12482, 8419, 20484, 16421, 28742, 24679, 33721, 37784, 41979,
            46042, 49981, 54044, 58239, 62302, 689, 4752, 8947, 13010, 16949,
            21012, 25207, 29270, 46570, 42443, 38312, 34185, 62830, 58703,
            54572, 50445, 13538, 9411, 5280, 1153, 29798, 25671, 21540, 17413,
            42971, 47098, 34713, 38840, 59231, 63358, 50973, 55100, 9939, 14066,
            1681, 5808, 26199, 30326, 17941, 22068, 55628, 51565, 63758, 59695,
            39368, 35305, 47498, 43435, 22596, 18533, 30726, 26663, 6336, 2273,
            14466, 10403, 52093, 56156, 60223, 64286, 35833, 39896, 43963,
            48026, 19061, 23124, 27191, 31254, 2801, 6864, 10931, 14994, 64814,
            60687, 56684, 52557, 48554, 44427, 40424, 36297, 31782, 27655,
            23652, 19525, 15522, 11395, 7392, 3265, 61215, 65342, 53085, 57212,
            44955, 49082, 36825, 40952, 28183, 32310, 20053, 24180, 11923,
            16050, 3793, 7920 };

    /**
     * NHACP CRC calculation support table
     */
    private static int[] nhacpCrcTable = new int[]
    { 0, 155, 173, 54, 193, 90, 108, 247, 25, 130, 180, 47, 216, 67, 117, 238,
            50, 169, 159, 4, 243, 104, 94, 197, 43, 176, 134, 29, 234, 113, 71,
            220, 100, 255, 201, 82, 165, 62, 8, 147, 125, 230, 208, 75, 188, 39,
            17, 138, 86, 205, 251, 96, 151, 12, 58, 161, 79, 212, 226, 121, 142,
            21, 35, 184, 200, 83, 101, 254, 9, 146, 164, 63, 209, 74, 124, 231,
            16, 139, 189, 38, 250, 97, 87, 204, 59, 160, 150, 13, 227, 120, 78,
            213, 34, 185, 143, 20, 172, 55, 1, 154, 109, 246, 192, 91, 181, 46,
            24, 131, 116, 239, 217, 66, 158, 5, 51, 168, 95, 196, 242, 105, 135,
            28, 42, 177, 70, 221, 235, 112, 11, 144, 166, 61, 202, 81, 103, 252,
            18, 137, 191, 36, 211, 72, 126, 229, 57, 162, 148, 15, 248, 99, 85,
            206, 32, 187, 141, 22, 225, 122, 76, 215, 111, 244, 194, 89, 174,
            53, 3, 152, 118, 237, 219, 64, 183, 44, 26, 129, 93, 198, 240, 107,
            156, 7, 49, 170, 68, 223, 233, 114, 133, 30, 40, 179, 195, 88, 110,
            245, 2, 153, 175, 52, 218, 65, 119, 236, 27, 128, 182, 45, 241, 106,
            92, 199, 48, 171, 157, 6, 232, 115, 69, 222, 41, 178, 132, 31, 167,
            60, 10, 145, 102, 253, 203, 80, 190, 37, 19, 136, 127, 228, 210, 73,
            149, 14, 56, 163, 84, 207, 249, 98, 140, 23, 33, 186, 77, 214, 224,
            123 };

    /**
     * Calculate the Cycle CRC based on the passed in byte array
     *
     * @param bytes data to calculate the CRC
     * @return Upper and lower bytes for use with nabu packets
     */
    public static byte[] calculateCycleCRC(List<Byte> bytes)
    {
        int seed = 0xFFFF;

        for (Byte b : bytes)
        {
            int index = (((seed >> 8)) ^ b.intValue()) & 0xFF;
            seed <<= 8;
            seed ^= cycleCrcTable[index];
        }

        // ok, now get the high and low order CRC bytes
        seed ^= 0xFFFF;
        byte[] crcArray = new byte[2];
        crcArray[0] = (byte) ((seed >> 8) & 0xFF);
        crcArray[1] = (byte) (seed & 0xFF);
        return crcArray;
    }

    /**
     * Calculate the CRC-8/CDMA2000 for NHACP
     * 
     * @param bytes data to calculate the CRC
     * @return int CRC value
     */
    public static int calculateNhacpCRC(byte[] bytes)
    {
        Integer seed = 0xFF;

        for (byte b : bytes)
        {
            int index = (seed ^ b) & 0xFF;
            seed = nhacpCrcTable[index] ^ (seed >> 8);
        }

        return seed;
    }
}
