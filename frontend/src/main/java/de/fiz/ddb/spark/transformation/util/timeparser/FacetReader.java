package de.fiz.ddb.spark.transformation.util.timeparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Reads facets from a file.
 * </p>
 * <ul>
 * <li>The first line in the file is ignored and can be used as a header line.</li>
 * <li>Each line must consist of seven columns, separated by a tab character (\t). The columns must correspond to the following, in the specified order:
 * <ol>
 * <li>ID</li>
 * <li>Notation</li>
 * <li>Earliest date in years</li>
 * <li>Latest date in years</li>
 * <li>German description</li>
 * <li>English description</li>
 * <li>A sorting value to sort facets</li>
 * </ol>
 * </li>
 * </ul>
 */
public class FacetReader {

    private boolean skipHashes = true;

    public List<Facet> read(String path, String charsetName) throws IOException, FacetReaderException {
        ArrayList<Facet> facets = new ArrayList<>();

        try (InputStream in = FacetReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new FacetReaderException("Facet file does could not be found for the given path \"" + path + "\"");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, charsetName))) {
                int i = 0;
                String line = null;
                boolean skippedFirstLine = false;
                while ( (line = reader.readLine()) != null ) {
                    if ( skippedFirstLine ) {
                        line = line.trim();
                        if ( line.length() > 0 ) {
                            String[] columns = line.split("\t");
                            if ( columns.length < 7 ) {
                                throw new FacetReaderException("Expected 7 columns instead of "
                                    + columns.length
                                    + " in facet file \""
                                    + path
                                    + "\", line "
                                    + i
                                    + ": \""
                                    + line
                                    + "\"");
                            }

                            try {
                                Facet facet =
                                    new Facet(columns[0], columns[1], Long.parseLong(columns[2]), Long.parseLong(columns[3]), columns[4], columns[5], columns[6]);
                                facets.add(facet);
                            } catch ( Exception x ) {
                                if ( !this.skipHashes ) {
                                    throw new FacetReaderException("Incorrect number format in facet file \"" + path + "\", line " + i + ": \"" + line + "\"");
                                }
                            }
                        }
                    } else {
                        skippedFirstLine = true;
                    }
                    i++;
                }
            }
        }

        return facets;
    }

    public List<Facet> readSkipHashFacets(String path, String charsetName) throws IOException, FacetReaderException {
        this.skipHashes = true;
        return read(path, charsetName);
    }
}
