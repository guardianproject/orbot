#!/usr/local/bin/perl

use strict;
use warnings;

sub main() {
    my $hit_header = 0;
    my $hit_option = 0;
    my $header_len;

    while (<>) {
        s/^1\. \@\@TITLE\@\@/     /i;

        if (m/^(\d\.)(\d\.)(\d\.)?\s/) {
            # Remove the first digit as it's the
            # config file section in the User Manual.
            s/^(\d\.)//;

            # If it's a section header, uppercase it.
            $_ = uc() if (/^\d\.\s+/);

            # Remember to underline it.
            $hit_header = 1;
            $header_len = length($_);
        }

        s/^/#  /;

        # XXX: someone should figure out what this stuff
        # is supposed to do (and if it really does that).
        s/^#  #/####/ if /^#  #{12,}/;
        s/^.*$// if $hit_option;
        $hit_option = 0;
        s/^\n//;
        s/^#\s*-{20,}//;
        s/ *$//;
        $hit_option = 1 if s/^#\s+@@//;
    
        print;

        if ($hit_header) {
            # The previous line was a section
            # header so we better underline it.
            die "Invalid header length" unless defined $header_len;
            print "#  " . "=" x $header_len . "\n";
            $hit_header = 0;
        };
    }
}
main();
