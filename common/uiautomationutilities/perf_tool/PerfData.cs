﻿//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

namespace PerfClTool.Measurement
{
    internal class PerfData
    {
        public List<PerfDataRecord> PerfDataRecordsList = new List<PerfDataRecord>();
        private PerfDataRecord _headers;

        public PerfData(string filePath)
        {
            if (!File.Exists(filePath))
            {
                throw new Exception($"File not found {filePath}");
            }

            PerfDataRecordsList = File.ReadLines(filePath)
                .Select(line => line.Split(','))
                .Select(tokens => new PerfDataRecord(
                    tokens[MapHeaderToIndex("TimeStamp")],
                    tokens[MapHeaderToIndex("Marker")],
                    tokens[MapHeaderToIndex("Time")],
                    tokens[MapHeaderToIndex("Thread")],
                    tokens[MapHeaderToIndex("CpuUsed")],
                    tokens[MapHeaderToIndex("CpuTotal")],
                    tokens[MapHeaderToIndex("ResidentSize")],
                    tokens[MapHeaderToIndex("VirtualSize")],
                    tokens[MapHeaderToIndex("WifiSent")],
                    tokens[MapHeaderToIndex("WifiRecv")],
                    tokens[MapHeaderToIndex("WwanSent")],
                    tokens[MapHeaderToIndex("WwanRecv")],
                    tokens[MapHeaderToIndex("AppSent")],
                    tokens[MapHeaderToIndex("AppRecv")],
                    tokens[MapHeaderToIndex("Battery")],
                    tokens[MapHeaderToIndex("SystemDiskRead")],
                    tokens[MapHeaderToIndex("SystemDiskWrite")]
                    )
                ).ToList();

            //Check if header is present
            if (PerfDataRecordsList.ElementAt(0).TimeStamp.Equals("TimeStamp"))
            {
                _headers = PerfDataRecordsList.ElementAt(0);
                PerfDataRecordsList.RemoveAt(0);
            }
        }

        private int MapHeaderToIndex(string token)
        {
            switch (token)
            {
                case "TimeStamp": return 0;
                case "Marker": return 1;
                case "Time": return 2;
                case "Thread": return 3;
                case "CpuUsed": return 4;
                case "CpuTotal": return 5;
                case "ResidentSize": return 6;
                case "VirtualSize": return 7;
                case "WifiSent": return 8;
                case "WifiRecv": return 9;
                case "WwanSent": return 10;
                case "WwanRecv": return 11;
                case "AppSent": return 12;
                case "AppRecv": return 13;
                case "Battery": return 14;
                case "SystemDiskRead": return 15;
                case "SystemDiskWrite": return 16;
                default: return -1;
            }
        }

        /// <summary>
        /// search for a marker
        /// </summary>
        /// <param name="marker"></param>
        /// <param name="skipCount"></param>
        /// <returns></returns>
        public PerfDataRecord FindMarker(String marker, int skipCount = 0)
        {
            var records = PerfDataRecordsList.Where(t =>
            t.Marker.Equals(marker, StringComparison.InvariantCultureIgnoreCase));
            if (records.Count() > skipCount)
            {
                return records.ElementAt(skipCount);
            }
            else
            {
                return null;
            }
        }

        public void AddMarkerNames()
        {
            _headers.MarkerName = "MarkerName";
        }

        /// <summary>
        /// time elapsed
        /// </summary>
        public void AdjustTimeElapsed()
        {
            var firstMarkerTimeElapsed = Int64.Parse(PerfDataRecordsList.ElementAt(0).Time);
            if (firstMarkerTimeElapsed != 0)
            {
                foreach (var perfRecord in PerfDataRecordsList)
                {
                    perfRecord.Time = (Int64.Parse(perfRecord.Time) - firstMarkerTimeElapsed).ToString();
                }
            }
        }

        /// <summary>
        ///  writing markers headers to file
        /// </summary>
        /// <param name="filePath"></param>
        public static void AppendAllHeadersToFile(String filePath)
        {
            StringBuilder sb = new StringBuilder();
            //Add Headers
            sb.AppendLine("Marker,MarkerName,Time(ms),Thread,ResidentSize,VirtualSize");
            File.AppendAllText(filePath, sb.ToString());
        }

        /// <summary>
        /// writing markers data to file
        /// </summary>
        /// <param name="filePath"></param>
        public void AppendMarkersDataToFile(string filePath)
        {
            var sb = new StringBuilder();
            foreach (var dataRecord in PerfDataRecordsList)
            {
                sb.AppendLine($"{dataRecord.Marker},{dataRecord.MarkerName},{long.Parse(dataRecord.Time) / 1000}," +
                    $"{dataRecord.Thread},{dataRecord.ResidentSize},{dataRecord.VirtualSize}");
            }
            File.AppendAllText(filePath, sb.ToString());
        }

        /// <summary>
        /// add creation time
        /// </summary>
        /// <param name="startTime"></param>
        public void AddPidCreationTime(DateTime startTime)
        {
            var firstRecord = PerfDataRecordsList.ElementAt(0);
            var timeDifference = DateTime.Parse(firstRecord.TimeStamp) - startTime;

            var logcatRecord = new PerfDataRecord()
            {
                TimeStamp = startTime.ToString("yyyy-MM-ddTHH:mm:ss.fff"),
                Marker = "logcat",
                Time = Convert.ToString(Convert.ToInt64(firstRecord.Time) - timeDifference.TotalMilliseconds * 1000, CultureInfo.InvariantCulture),
                Thread = firstRecord.Thread,
                CpuUsed = firstRecord.CpuUsed.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuUsed : "0",
                CpuTotal = firstRecord.CpuTotal.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuTotal : "0",
                ResidentSize = firstRecord.ResidentSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.ResidentSize : "0",
                VirtualSize = firstRecord.VirtualSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.VirtualSize : "0",
                WifiSent = firstRecord.WifiSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiSent : "0",
                WifiRecv = firstRecord.WifiRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiRecv : "0",
                WwanSent = firstRecord.WwanSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanSent : "0",
                WwanRecv = firstRecord.WwanRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanRecv : "0",
                AppSent = firstRecord.AppSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppSent : "0",
                AppRecv = firstRecord.AppRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppRecv : "0",
                Battery = firstRecord.Battery.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.Battery : "0",
                SystemDiskRead = firstRecord.SystemDiskRead.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskRead : "0",
                SystemDiskWrite = firstRecord.SystemDiskWrite.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskWrite : "0"
            };
            PerfDataRecordsList.Insert(0, logcatRecord);
        }

        /// <summary>
        /// add activity display time
        /// </summary>
        /// <param name="activityDisplayTimeMs"></param>
        public void AddActivityDisplayTime(long activityDisplayTimeMs)
        {
            var firstRecord = PerfDataRecordsList.ElementAt(0);
            var logcatRecord = new PerfDataRecord()
            {
                TimeStamp = DateTime.Parse(firstRecord.TimeStamp).AddMilliseconds(activityDisplayTimeMs).ToString("yyyy-MM-ddTHH:mm:ss.fff"),
                Marker = "activityDisplay",
                Time = Convert.ToString(Convert.ToInt64(firstRecord.Time) + activityDisplayTimeMs * 1000, CultureInfo.InvariantCulture),
                Thread = firstRecord.Thread,
                CpuUsed = firstRecord.CpuUsed.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuUsed : "0",
                CpuTotal = firstRecord.CpuTotal.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuTotal : "0",
                ResidentSize = firstRecord.ResidentSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.ResidentSize : "0",
                VirtualSize = firstRecord.VirtualSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.VirtualSize : "0",
                WifiSent = firstRecord.WifiSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiSent : "0",
                WifiRecv = firstRecord.WifiRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiRecv : "0",
                WwanSent = firstRecord.WwanSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanSent : "0",
                WwanRecv = firstRecord.WwanRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanRecv : "0",
                AppSent = firstRecord.AppSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppSent : "0",
                AppRecv = firstRecord.AppRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppRecv : "0",
                Battery = firstRecord.Battery.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.Battery : "0",
                SystemDiskRead = firstRecord.SystemDiskRead.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskRead : "0",
                SystemDiskWrite = firstRecord.SystemDiskWrite.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskWrite : "0"
            };
            PerfDataRecordsList.Insert(1, logcatRecord);
        }
    }
}
