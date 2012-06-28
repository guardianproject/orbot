#!/usr/bin/perl

# $Id: changelog2doc.pl,v 1.2 2008/09/26 16:49:09 fabiankeil Exp $
# $Source: /cvsroot/ijbswa/current/utils/changelog2doc.pl,v $

# Filter to parse the ChangeLog and translate the changes for
# the most recent version into something that looks like markup
# for the documentation but still needs fine-tuning.

use strict;
use warnings;

my @entries;

sub read_entries() {
    my $section_reached = 0;
    my $i = -1;

    while (<>) {
        if (/^\*{3} /) {
            last if $section_reached;
            $section_reached = 1;
            next;
        }
        next unless $section_reached;
        next if /^\s*$/;

        if (/^-/) {
            $i++; 
            $entries[$i] = '';
        }
        s@^-?\s*@@;

        $entries[$i] .= $_;
    }
    print "Parsed " . @entries . " entries.\n";
}

sub generate_markup() {
    my $markup = '';

    $markup .= "<para>\n" .
               " <itemizedlist>\n";

    foreach my $entry (@entries) {
        chomp $entry;
        $entry =~ s@\n@\n    @g;
        $markup .= "  <listitem>\n" .
                   "   <para>\n" .
                   "    " . $entry . "\n" .
                   "   </para>\n" .
                   "  </listitem>\n"
                   ;
    }
    $markup .= " </itemizedlist>\n" .
               "</para>\n";

    print $markup;
}

sub main () {
    read_entries();
    generate_markup();
}

main();
