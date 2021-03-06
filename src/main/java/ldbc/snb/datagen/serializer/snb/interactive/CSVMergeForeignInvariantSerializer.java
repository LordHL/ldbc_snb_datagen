/* 
 Copyright (c) 2013 LDBC
 Linked Data Benchmark Council (http://www.ldbcouncil.org)
 
 This file is part of ldbc_snb_datagen.
 
 ldbc_snb_datagen is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 ldbc_snb_datagen is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with ldbc_snb_datagen.  If not, see <http://www.gnu.org/licenses/>.
 
 Copyright (C) 2011 OpenLink Software <bdsmt@openlinksw.com>
 All Rights Reserved.
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation;  only Version 2 of the License dated
 June 1991.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.*/
package ldbc.snb.datagen.serializer.snb.interactive;

import ldbc.snb.datagen.dictionary.Dictionaries;
import ldbc.snb.datagen.objects.Organization;
import ldbc.snb.datagen.objects.Place;
import ldbc.snb.datagen.objects.Tag;
import ldbc.snb.datagen.objects.TagClass;
import ldbc.snb.datagen.serializer.HDFSCSVWriter;
import ldbc.snb.datagen.serializer.InvariantSerializer;
import ldbc.snb.datagen.vocabulary.DBP;
import ldbc.snb.datagen.vocabulary.DBPOWL;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by aprat on 17/02/15.
 */
public class CSVMergeForeignInvariantSerializer extends InvariantSerializer {

    private HDFSCSVWriter[] writers;

    private enum FileNames {
        TAG("tag"),
        TAGCLASS("tagclass"),
        PLACE("place"),
        ORGANIZATION("organisation");

        private final String name;

        private FileNames(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    @Override
    public void initialize(Configuration conf, int reducerId) throws IOException {
        int numFiles = FileNames.values().length;
        writers = new HDFSCSVWriter[numFiles];
        for (int i = 0; i < numFiles; ++i) {
            writers[i] = new HDFSCSVWriter(conf.get("ldbc.snb.datagen.serializer.socialNetworkDir"), FileNames
                    .values()[i].toString() + "_" + reducerId, conf.getInt("ldbc.snb.datagen.numPartitions", 1), conf
                                                   .getBoolean("ldbc.snb.datagen.serializer.compressed", false), "|", conf
                                                   .getBoolean("ldbc.snb.datagen.serializer.endlineSeparator", false));
        }

        ArrayList<String> arguments = new ArrayList<String>();
        arguments.add("id");
        arguments.add("name");
        arguments.add("url");
        arguments.add("hasType");
        writers[FileNames.TAG.ordinal()].writeHeader(arguments);

        arguments.clear();
        arguments.add("id");
        arguments.add("name");
        arguments.add("url");
        arguments.add("isSubclassOf");
        writers[FileNames.TAGCLASS.ordinal()].writeHeader(arguments);

        arguments.clear();
        arguments.add("id");
        arguments.add("name");
        arguments.add("url");
        arguments.add("type");
        arguments.add("isPartOf");
        writers[FileNames.PLACE.ordinal()].writeHeader(arguments);


        arguments.clear();
        arguments.add("id");
        arguments.add("type");
        arguments.add("name");
        arguments.add("url");
        arguments.add("place");
        writers[FileNames.ORGANIZATION.ordinal()].writeHeader(arguments);

    }

    public void close() {
        int numFiles = FileNames.values().length;
        for (int i = 0; i < numFiles; ++i) {
            writers[i].close();
        }
    }

    protected void serialize(final Place place) {
        ArrayList<String> arguments = new ArrayList<String>();
        arguments.add(Integer.toString(place.getId()));
        arguments.add(place.getName());
        arguments.add(DBP.getUrl(place.getName()));
        arguments.add(place.getType());

        if (place.getType() == Place.CITY ||
                place.getType() == Place.COUNTRY) {
            arguments.add(Integer.toString(Dictionaries.places.belongsTo(place.getId())));
        } else {
            arguments.add("");
        }
        writers[FileNames.PLACE.ordinal()].writeEntry(arguments);


    }

    protected void serialize(final Organization organization) {
        ArrayList<String> arguments = new ArrayList<String>();
        arguments.add(Long.toString(organization.id));
        arguments.add(organization.type.toString());
        arguments.add(organization.name);
        arguments.add(DBP.getUrl(organization.name));
        arguments.add(Integer.toString(organization.location));
        writers[FileNames.ORGANIZATION.ordinal()].writeEntry(arguments);
    }

    protected void serialize(final TagClass tagClass) {
        ArrayList<String> arguments = new ArrayList<String>();
        arguments.add(Integer.toString(tagClass.id));
        arguments.add(tagClass.name);
        if (tagClass.name.equals("Thing")) {
            arguments.add("http://www.w3.org/2002/07/owl#Thing");
        } else {
            arguments.add(DBPOWL.getUrl(tagClass.name));
        }
        if (tagClass.parent != -1) {
            arguments.add(Integer.toString(tagClass.parent));
        } else {
            arguments.add("");
        }
        writers[FileNames.TAGCLASS.ordinal()].writeEntry(arguments);

    }

    protected void serialize(final Tag tag) {
        ArrayList<String> arguments = new ArrayList<String>();
        arguments.add(Integer.toString(tag.id));
        arguments.add(tag.name);
        arguments.add(DBP.getUrl(tag.name));
        arguments.add(Integer.toString(tag.tagClass));
        writers[FileNames.TAG.ordinal()].writeEntry(arguments);
    }

    public void reset() {
        // Intentionally left empty
    }

}
