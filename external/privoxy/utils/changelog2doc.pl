#!/usr/bin/perl

##########################################################################
#
# $Id: changelog2doc.pl,v 1.8 2011/11/13 17:00:54 fabiankeil Exp $
#
# Filter to parse the ChangeLog and translate the changes for
# the most recent version into something that looks like markup
# for the documentation but still needs fine-tuning.
#
# Copyright (c) 2008, 2010 Fabian Keil <fk@fabiankeil.de>
#
# Permission to use, copy, modify, and distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
#
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
# ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
# ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
# OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
##########################################################################

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

        if (/^(\s*)-/) {
            my $indentation = length($1);
            if ($i > 1 and $entries[$i]{indentation} > $indentation) {
                $entries[$i]{last_list_item} = 1;
            }
            $i++; 
            $entries[$i]{description} = '';
            $entries[$i]{indentation} = $indentation;
        }
        if (/:\s*$/) {
            $entries[$i]{list_header} = 1;
        }

        s@^\s*-?\s*@@;

        $entries[$i]{description} .= $_;
    }
    if ($entries[$i]{indentation} != 0) {
        $entries[$i]{last_list_item} = 1;
    }
    print "Parsed " . @entries . " entries.\n";
}

sub create_listitem_markup($) {
    my $entry = shift;
    my $description = $entry->{description};
    my $markup = '';
    my $default_lws = '  ';
    my $lws = $default_lws x ($entry->{indentation} ? 2 : 1);

    chomp $description;

    $description =~ s@\n@\n  ${lws}@g;

    $markup .= $lws . "<listitem>\n" .
               $lws . " <para>\n";

    $markup .= $lws . "  " . $description . "\n";

    if (defined $entry->{list_header}) {
        $markup .= $lws . "  <itemizedlist>\n";

    } else {
        if (defined $entry->{last_list_item}) {
            $markup .= $lws . " </para>\n";
            $markup .= $lws . " </listitem>\n";
            $markup .= $lws . "</itemizedlist>\n";
            $lws = $default_lws;
        }
        $markup .= $lws . " </para>\n" .
                   $lws . "</listitem>\n";
    }

    return $markup;
}

sub wrap_in_para_itemlist_markup($) {
    my $content = shift;
    my $markup = "<para>\n" .
                 " <itemizedlist>\n" .
                 "  $content" .
                 " </itemizedlist>\n" .
                 "</para>\n";
    return $markup;
}

sub generate_markup() {
    my $markup = '';

    foreach my $entry (@entries) {
        $markup .= create_listitem_markup(\%{$entry});
    }

    print wrap_in_para_itemlist_markup($markup);
}

sub main () {
    read_entries();
    generate_markup();
}

main();
