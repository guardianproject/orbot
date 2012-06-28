#!/usr/bin/perl

############################################################################
#
# url-pattern-translator
#
# Filters Privoxy action files and changes old-school URL patterns to
# use extended regular expressions for the host as well.
#
# While it works good enough to satisfy the regression tests in
# default.action.master, it isn't perfect and you should double-check
# the output and keep backups of your old action files.
#
# Usage:
#
# url-pattern-translator.pl old.action > new.action 
#
# Only convert your files once, or, as RoboCop used to say,
# there will be... trouble.
#
# $Id: url-pattern-translator.pl,v 1.4 2009/06/21 18:15:24 fabiankeil Exp $
#
# Copyright (c) 2008 Fabian Keil <fk@fabiankeil.de>
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
#
############################################################################

use strict;
use warnings;

sub p ($) {
    my $message = shift;
    print $message . "\n";
}

sub convert_host_pattern ($) {
    my $host_pattern = shift;
    my $hp = $host_pattern;

    $hp =~ s@\s@@g;

    if ($hp =~ m@^\.@) {
        # Not left-anchored
        #
        # XXX: This is somewhat ugly and while it's
        # the equivalent pattern in most cases
        # \. should be good enough.
        $hp =~ s@^\.@(^|.)@;
    } else {
        # left-anchored
        $hp = '^' . $hp;
    }

    # Match-all syntax has changed ...
    $hp =~ s@\*@[^.]*@g;

    # Extended host patterns are right-anchored by default
    $hp =~ s@\.$@(\..*)?@;

    # Literal dots have to be escaped    
    $hp =~ s@((?<!\\)\.[^*])@\\$1@g;

    # Match single character with a dot.
    $hp =~ s@(?<!\))\?@.@g;

    return $hp;
}

sub looks_interesting($) {
    my $line = shift;
    my $type_to_skip = undef;

    if (/^\s*\#/) {

        $type_to_skip = "comment";

    } elsif (/[{}]/ or /\\$/) {

        $type_to_skip = "action settings";

    } elsif (m@^\s*$@) {

        $type_to_skip = "whitespace";

    } elsif (m@^\s*TAG:@) {

        $type_to_skip = "tag patttern";

    } elsif (m@^[^/]*=@) {

        $type_to_skip = "macro or version definition";

    } elsif (m@^\s*standard\.@) {

        $type_to_skip = "predefined settings";

    }

    #p("Skipping " . $type_to_skip . ": " . $_) if defined $type_to_skip;

    return not defined $type_to_skip;
}

sub main () {
    my $host = undef;
    my $path = undef;
 
    while (<>) {
        chomp;

        if (looks_interesting($_)) {
            if (m@^([^/]+)(/.*)$@) {
                $host = $1;
                $path = $2;
                $host = convert_host_pattern($host);
                $_ = $host . $path;
            }
            elsif (m@^([^/]*)$@) {
                $host = $1;
                $host = convert_host_pattern($host);
                $_ = $host;
            }
        }
        p($_);
    }
}

main();
