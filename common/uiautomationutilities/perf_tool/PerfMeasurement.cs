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

using System.Collections.Generic;
using System.Linq;

namespace PerfClTool.Measurement
{
    internal abstract class IMeasurement
    {
        public ResponseTimeMetric ResponseTime { get; set; }
        public VssBeginMetric VssBegin { get; set; }
        public VssEndMetric VssEnd { get; set; }
        public VssDeltaMetric VssDelta { get; set; }
        public RssBeginMetric RssBegin { get; set; }
        public RssEndMetric RssEnd { get; set; }
        public RssDeltaMetric RssDelta { get; set; }
    }

    internal class IterationMeasurement : IMeasurement
    {
        public IterationMeasurement(PerfDataRecord startMarker, PerfDataRecord endMarker)
        {
            ResponseTime = new ResponseTimeMetric(startMarker, endMarker);
            VssBegin = new VssBeginMetric(startMarker, endMarker);
            VssEnd = new VssEndMetric(startMarker, endMarker);
            VssDelta = new VssDeltaMetric(startMarker, endMarker);
            RssBegin = new RssBeginMetric(startMarker, endMarker);
            RssEnd = new RssEndMetric(startMarker, endMarker);
            RssDelta = new RssDeltaMetric(startMarker, endMarker);
        }
    }

    internal abstract class AggregatedMeasurement : IMeasurement
    {
        public AggregatedMeasurement(List<IterationMeasurement> rawIterationsData) { }
        public abstract string Name { get; }
    }

    /// <summary>
    /// Average
    /// </summary>
    internal class Average : AggregatedMeasurement
    {
        public Average(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList().Average());
            VssBegin = new VssBeginMetric(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList().Average());
            VssEnd = new VssEndMetric(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList().Average());
            VssDelta = new VssDeltaMetric(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList().Average());
            RssBegin = new RssBeginMetric(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList().Average());
            RssEnd = new RssEndMetric(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList().Average());
            RssDelta = new RssDeltaMetric(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList().Average());
        }

        public override string Name => "Average";
    }

    /// <summary>
    /// Number of iterations
    /// </summary>
    internal class NumIterations : AggregatedMeasurement
    {
        public NumIterations(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).Count());
            VssBegin = new VssBeginMetric(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).Count());
            VssEnd = new VssEndMetric(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).Count());
            VssDelta = new VssDeltaMetric(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).Count());
            RssBegin = new RssBeginMetric(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).Count());
            RssEnd = new RssEndMetric(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).Count());
            RssDelta = new RssDeltaMetric(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).Count());
        }

        public override string Name => "NumIterations";
    }

    /// <summary>
    /// STD deviation
    /// </summary>
    internal class Stdev : AggregatedMeasurement
    {
        public Stdev(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList()));
            VssBegin = new VssBeginMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList()));
            VssEnd = new VssEndMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList()));
            VssDelta = new VssDeltaMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList()));
            RssBegin = new RssBeginMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList()));
            RssEnd = new RssEndMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList()));
            RssDelta = new RssDeltaMetric(MathUtils.GetStdDeviation(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList()));
        }

        public override string Name => "Stdev";
    }

    /// <summary>
    /// Min
    /// </summary>
    internal class Min : AggregatedMeasurement
    {
        public Min(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList().Min());
            VssBegin = new VssBeginMetric(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList().Min());
            VssEnd = new VssEndMetric(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList().Min());
            VssDelta = new VssDeltaMetric(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList().Min());
            RssBegin = new RssBeginMetric(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList().Min());
            RssEnd = new RssEndMetric(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList().Min());
            RssDelta = new RssDeltaMetric(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList().Min());
        }

        public override string Name => "Min";
    }

    /// <summary>
    /// Max
    /// </summary>
    internal class Max : AggregatedMeasurement
    {
        public Max(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList().Max());
            VssBegin = new VssBeginMetric(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList().Max());
            VssEnd = new VssEndMetric(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList().Max());
            VssDelta = new VssDeltaMetric(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList().Max());
            RssBegin = new RssBeginMetric(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList().Max());
            RssEnd = new RssEndMetric(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList().Max());
            RssDelta = new RssDeltaMetric(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList().Max());
        }

        public override string Name => "Max";
    }

    /// <summary>
    /// 25th percentile
    /// </summary>
    internal class Percentile25 : AggregatedMeasurement
    {
        public Percentile25(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList(), 25));
            VssBegin = new VssBeginMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList(), 25));
            VssEnd = new VssEndMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList(), 25));
            VssDelta = new VssDeltaMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList(), 25));
            RssBegin = new RssBeginMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList(), 25));
            RssEnd = new RssEndMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList(), 25));
            RssDelta = new RssDeltaMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList(), 25));
        }

        public override string Name => "25Percentile";
    }

   /// <summary>
   /// 50th percentile
   /// </summary>
    internal class Percentile50 : AggregatedMeasurement
    {
        public Percentile50(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList(), 50));
            VssBegin = new VssBeginMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList(), 50));
            VssEnd = new VssEndMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList(), 50));
            VssDelta = new VssDeltaMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList(), 50));
            RssBegin = new RssBeginMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList(), 50));
            RssEnd = new RssEndMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList(), 50));
            RssDelta = new RssDeltaMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList(), 50));
        }

        public override string Name => "50Percentile";
    }

    /// <summary>
    /// 75th percentile
    /// </summary>
    internal class Percentile75 : AggregatedMeasurement
    {
        public Percentile75(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList(), 75));
            VssBegin = new VssBeginMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList(), 75));
            VssEnd = new VssEndMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList(), 75));
            VssDelta = new VssDeltaMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList(), 75));
            RssBegin = new RssBeginMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList(), 75));
            RssEnd = new RssEndMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList(), 75));
            RssDelta = new RssDeltaMetric(MathUtils.GetPercentile(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList(), 75));
        }

        public override string Name => "75Percentile";
    }

    /// <summary>
    /// Best 75th factor average
    /// </summary>
    internal class Best75Avg : AggregatedMeasurement
    {
        public Best75Avg(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList(), 75).Average());
            VssBegin = new VssBeginMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList(), 75).Average());
            VssEnd = new VssEndMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList(), 75).Average());
            VssDelta = new VssDeltaMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList(), 75).Average());
            RssBegin = new RssBeginMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList(), 75).Average());
            RssEnd = new RssEndMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList(), 75).Average());
            RssDelta = new RssDeltaMetric(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList(), 75).Average());
        }

        public override string Name => "75BestAvg";
    }

    /// <summary>
    /// Best 75th factor STD deviation
    /// </summary>
    internal class Best75Stdev : AggregatedMeasurement
    {
        public Best75Stdev(List<IterationMeasurement> rawIterationsData) : base(rawIterationsData)
        {
            ResponseTime = new ResponseTimeMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.ResponseTime.MeasurementValue).ToList(), 75)));
            VssBegin = new VssBeginMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.VssBegin.MeasurementValue).ToList(), 75)));
            VssEnd = new VssEndMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.VssEnd.MeasurementValue).ToList(), 75)));
            VssDelta = new VssDeltaMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.VssDelta.MeasurementValue).ToList(), 75)));
            RssBegin = new RssBeginMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.RssBegin.MeasurementValue).ToList(), 75)));
            RssEnd = new RssEndMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.RssEnd.MeasurementValue).ToList(), 75)));
            RssDelta = new RssDeltaMetric(MathUtils.GetStdDeviation(MathUtils.GetMinSdSubset(rawIterationsData.Select(t => t.RssDelta.MeasurementValue).ToList(), 75)));
        }

        public override string Name => "75BestStdev";
    }
}
