# gpxmergeheartrate

[![Java CI with Maven](https://github.com/ptanov/gpxmergeheartrate/workflows/Java%20CI%20with%20Maven/badge.svg)][actions-link]

[actions-link]: https://github.com/ptanov/gpxmergeheartrate/actions "Configured GitHub Actions"

This program merges gpx track with heart rate provided by MiBand3 (and exported by [Tools & Mi Band/Mi Band Tools](https://play.google.com/store/apps/details?id=cz.zdenekhorak.mibandtools) or [Notify & Fitness for Mi Band](https://play.google.com/store/apps/details?id=com.mc.miband1&hl=en)).
There is a docker image here: <https://hub.docker.com/r/ptanov/gpxmergeheartrate>. The source code is available here: <https://github.com/ptanov/gpxmergeheartrate>.

## Running

### Java

- `mvn package exec:java -Dexec.args='"<input gpx file>" "<input csv heart rate file>" "<result gpx file>"'`
- `mvn package && java -jar target/eu.tanov.gps.gpxmergeheartrate-1.0-SNAPSHOT.jar "<input gpx file>" "<input csv heart rate file>" "<result gpx file>"`

### Docker

- **simple:** gpx file with csv file in current directory, fixing the owner, resulting file is named based on current directory: `docker run --rm -it -v /etc/timezone:/etc/timezone:ro -v "$(pwd)":/data ptanov/gpxmergeheartrate *.gpx *.csv out && cp out "${PWD##*/}.gpx" && rm -f out`
- if files are in current directory: `docker run --rm -it -v /etc/timezone:/etc/timezone:ro -v "$(pwd)":/data ptanov/gpxmergeheartrate "input.gpx" "input.csv" "output.gpx"`
- if files are in any folder: `docker run --rm -it -v /etc/timezone:/etc/timezone:ro -v <folderofinputfiles>:<folderofinputfiles> ptanov/gpxmergeheartrate "<input gpx file>" "<input csv heart rate file>" "<result gpx file>"`
- make sure that you have mounted the volume with input/output files using `-v`, e.g. `docker run --rm -it -v /home/user/gpx:/home/user/gpx ptanov/gpxmergeheartrate "/home/user/gpx/input.gpx" "/home/user/gpx/hr.csv" "/home/user/gpx/result.gpx"`
- keep in mind the docker's timezone ( <https://forums.docker.com/t/synchronize-timezone-from-host-to-container/39116> )

## Development and maven

- `mvn eclipse:eclipse`
- `mvn test`
- `mvn package`
- `docker build -t ptanov/gpxmergeheartrate .`

## Sample files

### Input GPX file

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<gpx
	xmlns="http://www.topografix.com/GPX/1/1"
	xmlns:gpxx="http://www.garmin.com/xmlschemas/GpxExtensions/v3"
	xmlns:wptx1="http://www.garmin.com/xmlschemas/WaypointExtension/v1"
	xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
	creator="Dakota 20"
	version="1.1"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/WaypointExtension/v1 http://www8.garmin.com/xmlschemas/WaypointExtensionv1.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd">
	<metadata>
		<link href="http://www.garmin.com">
			<text>Garmin International</text>
		</link>
		<time>2018-11-15T11:44:24Z</time>
	</metadata>
	<trk>
		<name>Day 10-NOV-18 07:24:58</name>
		<extensions>
			<gpxx:TrackExtension>
				<gpxx:DisplayColor>Cyan</gpxx:DisplayColor>
			</gpxx:TrackExtension>
		</extensions>
		<trkseg>
			<trkpt lat="42.5831057969" lon="23.2922615111"><ele>1807.42</ele><time>2018-11-10T05:24:58Z</time></trkpt>
		</trkseg>
	</trk>
</gpx>
```

### Input CSV heart rate file (in the device timezone)

```csv
dateTime,rate,rateZone
10.11.2018 07:24:58,83,43%
```

### Result GPX file

```xml
<?xml version="1.0" encoding="UTF-8"?><gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxx="http://www.garmin.com/xmlschemas/GpxExtensions/v3" xmlns:wptx1="http://www.garmin.com/xmlschemas/WaypointExtension/v1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" creator="Dakota 20" version="1.1" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/WaypointExtension/v1 http://www8.garmin.com/xmlschemas/WaypointExtensionv1.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd">
	<metadata>
		<link href="http://www.garmin.com">
			<text>Garmin International</text>
		</link>
		<time>2018-11-15T11:44:24Z</time>
	</metadata>
	<trk>
		<name>Day 10-NOV-18 07:24:58</name>
		<extensions>
			<gpxx:TrackExtension>
				<gpxx:DisplayColor>Cyan</gpxx:DisplayColor>
			</gpxx:TrackExtension>
		</extensions>
		<trkseg>
			<trkpt lon="23.2922615111" lat="42.5831057969">
				<ele>1807.42</ele>
				<time>2018-11-10T05:24:58Z</time>
				<extensions>
					<gpxtpx:TrackPointExtension>
						<gpxtpx:hr>83</gpxtpx:hr>
					</gpxtpx:TrackPointExtension>
				</extensions>
			</trkpt>
		</trkseg>
	</trk>
</gpx>
```
