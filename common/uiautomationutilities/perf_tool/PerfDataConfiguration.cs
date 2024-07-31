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
using System.Xml.Serialization;

namespace PerfClTool.Measurement
{
    /// <summary>
    /// configuration
    /// </summary>
    [XmlRoot(ElementName = "MeasurementConfiguration")]
    public class MeasurementConfiguration
    {
        [XmlAttribute(AttributeName = "Id")]
        public string Id { get; set; }
        [XmlAttribute(AttributeName = "StartMarker")]
        public string StartMarker { get; set; }
        [XmlAttribute(AttributeName = "EndMarker")]
        public string EndMarker { get; set; }
        [XmlAttribute(AttributeName = "startskip")]
        public string Startskip { get; set; }
        [XmlAttribute(AttributeName = "EndSkip")]
        public string EndSkip { get; set; }
        [XmlAttribute(AttributeName = "Name")]
        public string Name { get; set; }
    }

    [XmlRoot(ElementName = "MeasurementsConfigurations")]
    public class MeasurementsConfigurations
    {
        [XmlElement(ElementName = "MeasurementConfiguration")]
        public List<MeasurementConfiguration> MeasurementConfiguration { get; set; }
    }

    /// <summary>
    /// measurement
    /// </summary>
    [XmlRoot(ElementName = "Measurement")]
    public class Measurement
    {
        [XmlAttribute(AttributeName = "Id")]
        public string Id { get; set; }
        [XmlAttribute(AttributeName = "IsPrimary")]
        public string IsPrimary { get; set; }
    }

    [XmlRoot(ElementName = "Measurements")]
    public class Measurements
    {
        [XmlElement(ElementName = "Measurement")]
        public List<Measurement> Measurement { get; set; }
    }

    [XmlRoot(ElementName = "Scenario")]
    public class Scenario
    {
        [XmlElement(ElementName = "Name")]
        public string Name { get; set; }
        [XmlElement(ElementName = "Measurements")]
        public Measurements Measurements { get; set; }
    }

    [XmlRoot(ElementName = "Scenarios")]
    public class Scenarios
    {
        [XmlElement(ElementName = "Scenario")]
        public List<Scenario> Scenario { get; set; }
    }

    [XmlRoot(ElementName = "App")]
    public class App
    {
        [XmlElement(ElementName = "Name")]
        public string Name { get; set; }
        [XmlElement(ElementName = "Scenarios")]
        public Scenarios Scenarios { get; set; }
    }

    [XmlRoot(ElementName = "Apps")]
    public class Apps
    {
        [XmlElement(ElementName = "App")]
        public List<App> App { get; set; }
    }

    [XmlRoot(ElementName = "ScenarioMeasurementsMapping")]
    public class ScenarioMeasurementsMapping
    {
        [XmlElement(ElementName = "Apps")]
        public Apps Apps { get; set; }
    }

    /// <summary>
    /// configuration
    /// </summary>
    [XmlRoot(ElementName = "PerfDataConfiguration")]
    public class PerfDataConfiguration
    {
        [XmlElement(ElementName = "MeasurementsConfigurations")]
        public MeasurementsConfigurations MeasurementsConfigurations { get; set; }
        [XmlElement(ElementName = "ScenarioMeasurementsMapping")]
        public ScenarioMeasurementsMapping ScenarioMeasurementsMapping { get; set; }
    }


}
