#!/usr/bin/perl

############################################################################
#
# Privoxy-Regression-Test
#
# A regression test "framework" for Privoxy. For documentation see:
# perldoc privoxy-regression-test.pl
#
# $Id: privoxy-regression-test.pl,v 1.81 2011/10/30 16:22:29 fabiankeil Exp $
#
# Wish list:
#
# - Update documentation
# - Validate HTTP times.
# - Implement a HTTP_VERSION directive or allow to
#   specify whole request lines.
# - Support filter regression tests.
# - Document magic Expect Header values
# - Internal fuzz support?
#
# Copyright (c) 2007-2011 Fabian Keil <fk@fabiankeil.de>
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

use warnings;
use strict;
use Getopt::Long;

use constant {
    PRT_VERSION => 'Privoxy-Regression-Test 0.5',
 
    CURL => 'curl',

    # CLI option defaults
    CLI_RETRIES   => 1,
    CLI_LOOPS     => 1,
    CLI_MAX_TIME  => 5,
    CLI_MIN_LEVEL => 0,
    # XXX: why limit at all?
    CLI_MAX_LEVEL => 100,
    CLI_FORKS     => 0,
    CLI_SLEEP_TIME => 0,

    PRIVOXY_CGI_URL  => 'http://p.p/',
    FELLATIO_URL     => 'http://127.0.0.1:8080/',
    LEADING_LOG_DATE => 1,
    LEADING_LOG_TIME => 1,

    DEBUG_LEVEL_FILE_LOADING    => 0,
    DEBUG_LEVEL_PAGE_FETCHING   => 0,
    DEBUG_LEVEL_VERBOSE_FAILURE => 1,
    # XXX: Only partly implemented and mostly useless.
    DEBUG_LEVEL_VERBOSE_SUCCESS => 0,
    DEBUG_LEVEL_STATUS          => 1,

    VERBOSE_TEST_DESCRIPTION    => 1,

    # Internal use, don't modify
    # Available debug bits:
    LL_SOFT_ERROR       =>  1,
    LL_VERBOSE_FAILURE  =>  2,
    LL_PAGE_FETCHING    =>  4,
    LL_FILE_LOADING     =>  8,
    LL_VERBOSE_SUCCESS  => 16,
    LL_STATUS           => 32,

    CLIENT_HEADER_TEST  =>  1,
    SERVER_HEADER_TEST  =>  2,
    DUMB_FETCH_TEST     =>  3,
    METHOD_TEST         =>  4,
    STICKY_ACTIONS_TEST =>  5,
    TRUSTED_CGI_REQUEST =>  6,
    BLOCK_TEST          =>  7,
    REDIRECT_TEST       =>108,
};

sub init_our_variables () {

    our $leading_log_time = LEADING_LOG_TIME;
    our $leading_log_date = LEADING_LOG_DATE;
    our $privoxy_cgi_url  = PRIVOXY_CGI_URL;
    our $verbose_test_description = VERBOSE_TEST_DESCRIPTION;
    our $log_level = get_default_log_level();
}

sub get_default_log_level () {
    
    my $log_level = 0;

    $log_level |= LL_FILE_LOADING    if DEBUG_LEVEL_FILE_LOADING;
    $log_level |= LL_PAGE_FETCHING   if DEBUG_LEVEL_PAGE_FETCHING;
    $log_level |= LL_VERBOSE_FAILURE if DEBUG_LEVEL_VERBOSE_FAILURE;
    $log_level |= LL_VERBOSE_SUCCESS if DEBUG_LEVEL_VERBOSE_SUCCESS;
    $log_level |= LL_STATUS          if DEBUG_LEVEL_STATUS;

    # This one is supposed to be always on.
    $log_level |= LL_SOFT_ERROR;

    return $log_level;
}

############################################################################
#
# File loading functions
#
############################################################################

sub parse_tag ($) {

    my $tag = shift;

    # Remove anchors
    $tag =~ s@[\$\^]@@g;
    # Unescape brackets and dots
    $tag =~ s@\\(?=[{}().+])@@g;

    # log_message("Parsed tag: " . $tag);

    check_for_forbidden_characters($tag);

    return $tag;
}

sub check_for_forbidden_characters ($) {

    my $string = shift;
    my $allowed = '[-=\dA-Za-z~{}:./();\t ,+@"_%?&*^]';

    unless ($string =~ m/^$allowed*$/o) {
        my $forbidden = $string;
        $forbidden =~ s@^$allowed*(.).*@$1@;

        log_and_die("'" . $string . "' contains character '" . $forbidden. "' which is unacceptable.");
    }
}

sub load_regression_tests() {
    if (cli_option_is_set('local-test-file')) {
        load_regression_tests_from_file(get_cli_option('local-test-file'));
    } else {
        load_regression_tests_through_privoxy();
    }
}

# XXX: Contains a lot of code duplicated from load_action_files()
#      that should be factored out.
sub load_regression_tests_from_file ($) {
    my $action_file = shift;

    # initialized here
    our %actions;
    our @regression_tests;

    my $si = 0;  # Section index
    my $ri = -1; # Regression test index
    my $count = 0;

    my $ignored = 0;

    my $sticky_actions = undef;

    l(LL_STATUS, "Gathering regression tests from local file " . $action_file);

    open(my $ACTION_FILE, "<", $action_file)
        or log_and_die("Failed to open $action_file: $!");

    while (<$ACTION_FILE>) {

        my $no_checks = 0;
        chomp;
        my ($token, $value) = tokenize($_);

        next unless defined $token;

        # Load regression tests

        if (token_starts_new_test($token)) {

            # Beginning of new regression test.
            $ri++;
            $count++;
            enlist_new_test(\@regression_tests, $token, $value, $si, $ri, $count);
            $no_checks = 1; # Already validated by enlist_new_test().
        }

        if ($token =~ /level\s+(\d+)/i) {

            my $level = $1;
            register_dependency($level, $value);
        }

        if ($token eq 'sticky actions') {

            # Will be used by each following Sticky URL.
            $sticky_actions = $value;
            if ($sticky_actions =~ /{[^}]*\s/) {
                log_and_die("'Sticky Actions' with whitespace inside the " .
                            "action parameters are currently unsupported.");
            }
        }

        if ($si == -1 || $ri == -1) {
            # No beginning of a test detected yet,
            # so we don't care about any other test
            # attributes.
            next;
        }

        if ($token eq 'expect header') {

            l(LL_FILE_LOADING, "Detected expectation: " . $value);
            $regression_tests[$si][$ri]{'expect-header'} = $value;

        } elsif ($token eq 'tag') {

            next if ($ri == -1);

            my $tag = parse_tag($value);

            # We already checked in parse_tag() after filtering
            $no_checks = 1;

            l(LL_FILE_LOADING, "Detected TAG: " . $tag);

            # Save tag for all tests in this section
            do {
                $regression_tests[$si][$ri]{'tag'} = $tag;
            } while ($ri-- > 0);

            $si++;
            $ri = -1;

        } elsif ($token eq 'ignore' && $value =~ /Yes/i) {

            l(LL_FILE_LOADING, "Ignoring section: " . test_content_as_string($regression_tests[$si][$ri]));
            $regression_tests[$si][$ri]{'ignore'} = 1;
            $ignored++;

        } elsif ($token eq 'expect status code') {

            l(LL_FILE_LOADING, "Expecting status code: " . $value);
            $regression_tests[$si][$ri]{'expected-status-code'} = $value;

        } elsif ($token eq 'level') { # XXX: stupid name

            $value =~ s@(\d+).*@$1@;
            l(LL_FILE_LOADING, "Level: " . $value);
            $regression_tests[$si][$ri]{'level'} = $value;

        } elsif ($token eq 'method') {

            l(LL_FILE_LOADING, "Method: " . $value);
            $regression_tests[$si][$ri]{'method'} = $value;

        } elsif ($token eq 'redirect destination') {

            l(LL_FILE_LOADING, "Redirect destination: " . $value);
            $regression_tests[$si][$ri]{'redirect destination'} = $value;

        } elsif ($token eq 'url') {

            if (defined $sticky_actions) {
                die "WTF? Attempted to overwrite Sticky Actions"
                    if defined ($regression_tests[$si][$ri]{'sticky-actions'});

                l(LL_FILE_LOADING, "Sticky actions: " . $sticky_actions);
                $regression_tests[$si][$ri]{'sticky-actions'} = $sticky_actions;
            } else {
                log_and_die("Sticky URL without Sticky Actions: $value");
            }

        } else {

            # We don't use it, so we don't need
            $no_checks = 1;
            l(LL_STATUS, "Enabling no_checks for $token") unless $no_checks;
        }

        # XXX: Necessary?
        unless ($no_checks)  {
            check_for_forbidden_characters($value);
            check_for_forbidden_characters($token);
        }
    }

    l(LL_FILE_LOADING, "Done loading " . $count . " regression tests."
      . " Of which " . $ignored. " will be ignored)\n");

}


sub load_regression_tests_through_privoxy () {

    our $privoxy_cgi_url;
    our @privoxy_config;
    our %privoxy_features;
    my @actionfiles;
    my $curl_url = '';
    my $file_number = 0;
    my $feature;

    $curl_url .= $privoxy_cgi_url;
    $curl_url .= 'show-status';

    l(LL_STATUS, "Asking Privoxy for the number of action files available ...");

    # Dear Privoxy, please reload the config file if necessary ...
    get_cgi_page_or_else($curl_url);

    # ... so we get the latest one here.
    foreach (@{get_cgi_page_or_else($curl_url)}) {

        chomp;
        if (/<td>(.*?)<\/td><td class=\"buttons\"><a href=\"\/show-status\?file=actions&amp;index=(\d+)\">/) {

            my $url = $privoxy_cgi_url . 'show-status?file=actions&index=' . $2;
            $actionfiles[$file_number++] = $url;

        } elsif (m@config\.html#.*\">([^<]*)</a>\s+(.*)<br>@) {

            my $directive = $1 . " " . $2;
            push (@privoxy_config, $directive);

        } elsif (m@<td><code>([^<]*)</code></td>@) {

            $feature = $1;

        } elsif (m@<td> (Yes|No) </td>@) {

            $privoxy_features{$feature} = $1 if defined $feature;
            $feature = undef;
        }
    }

    l(LL_FILE_LOADING, "Recognized " . @actionfiles . " actions files");

    load_action_files(\@actionfiles);
}

sub token_starts_new_test ($) {

    my $token = shift;
    my @new_test_directives = ('set header', 'fetch test',
         'trusted cgi request', 'request header', 'method test',
         'blocked url', 'url', 'redirected url');

    foreach my $new_test_directive (@new_test_directives) {
        return 1 if $new_test_directive eq $token;
    }

    return 0;
}

sub tokenize ($) {

    my ($token, $value) = (undef, undef);

    # Remove leading and trailing white space.
    s@^\s*@@;
    s@\s*$@@;

    # Reverse HTML-encoding
    # XXX: Seriously incomplete.
    s@&quot;@"@g;
    s@&amp;@&@g;

    # Tokenize
    if (/^\#\s*([^=:#]*?)\s*[=]\s*([^#]+)(?:#.*)?$/) {

        $token = $1;
        $value = $2;

        $token =~ s@\s\s+@ @g;
        $token =~ tr/[A-Z]/[a-z]/;

    } elsif (/^TAG\s*:(.*)$/) {

        $token = 'tag';
        $value = $1;
    }

    return ($token, $value);
}

sub enlist_new_test ($$$$$$) {

    my ($regression_tests, $token, $value, $si, $ri, $number) = @_;
    my $type;
    my $executor;

    if ($token eq 'set header') {

        l(LL_FILE_LOADING, "Header to set: " . $value);
        $type = CLIENT_HEADER_TEST;
        $executor = \&execute_client_header_regression_test;

    } elsif ($token eq 'request header') {

        l(LL_FILE_LOADING, "Header to request: " . $value);
        $type = SERVER_HEADER_TEST;
        $executor = \&execute_server_header_regression_test;
        $$regression_tests[$si][$ri]{'expected-status-code'} = 200;

    } elsif ($token eq 'trusted cgi request') {

        l(LL_FILE_LOADING, "CGI URL to test in a dumb way: " . $value);
        $type = TRUSTED_CGI_REQUEST;
        $executor = \&execute_dumb_fetch_test;
        $$regression_tests[$si][$ri]{'expected-status-code'} = 200;

    } elsif ($token eq 'fetch test') {

        l(LL_FILE_LOADING, "URL to test in a dumb way: " . $value);
        $type = DUMB_FETCH_TEST;
        $executor = \&execute_dumb_fetch_test;
        $$regression_tests[$si][$ri]{'expected-status-code'} = 200;

    } elsif ($token eq 'method test') {

        l(LL_FILE_LOADING, "Method to test: " . $value);
        $type = METHOD_TEST;
        $executor = \&execute_method_test;
        $$regression_tests[$si][$ri]{'expected-status-code'} = 200;

    } elsif ($token eq 'blocked url') {

        l(LL_FILE_LOADING, "URL to block-test: " . $value);
        $executor = \&execute_block_test;
        $type = BLOCK_TEST;

    } elsif ($token eq 'url') {

        l(LL_FILE_LOADING, "Sticky URL to test: " . $value);
        $type = STICKY_ACTIONS_TEST;
        $executor = \&execute_sticky_actions_test;

    } elsif ($token eq 'redirected url') {

        l(LL_FILE_LOADING, "Redirected URL to test: " . $value);
        $type = REDIRECT_TEST;
        $executor = \&execute_redirect_test;

    } else {

        die "Incomplete '" . $token . "' support detected."; 
    }

    $$regression_tests[$si][$ri]{'type'} = $type;
    $$regression_tests[$si][$ri]{'level'} = $type;
    $$regression_tests[$si][$ri]{'executor'} = $executor;

    check_for_forbidden_characters($value);

    $$regression_tests[$si][$ri]{'data'} = $value;

    # For function that only get passed single tests
    $$regression_tests[$si][$ri]{'section-id'} = $si;
    $$regression_tests[$si][$ri]{'regression-test-id'} = $ri;
    $$regression_tests[$si][$ri]{'number'} = $number - 1;
    l(LL_FILE_LOADING,
      "Regression test " . $number . " (section:" . $si . "):");
}

# XXX: Shares a lot of code with load_regression_tests_from_file()
#      that should be factored out.
sub load_action_files ($) {

    # initialized here
    our %actions;
    our @regression_tests;

    my $actionfiles_ref = shift;
    my @actionfiles = @{$actionfiles_ref};

    my $si = 0;  # Section index
    my $ri = -1; # Regression test index
    my $count = 0;

    my $ignored = 0;

    l(LL_STATUS, "Gathering regression tests from " .
      @actionfiles . " action file(s) delivered by Privoxy.");

    for my $file_number (0 .. @actionfiles - 1) {

        my $curl_url = quote($actionfiles[$file_number]);
        my $actionfile = undef;
        my $sticky_actions = undef;

        foreach (@{get_cgi_page_or_else($curl_url)}) {

            my $no_checks = 0;
            chomp;

            if (/<h2>Contents of Actions File (.*?)</) {
                $actionfile = $1;
                next;
            }
            next unless defined $actionfile;

            last if (/<\/pre>/);

            my ($token, $value) = tokenize($_);

            next unless defined $token;

            # Load regression tests

            if (token_starts_new_test($token)) {

                # Beginning of new regression test.
                $ri++;
                $count++;
                enlist_new_test(\@regression_tests, $token, $value, $si, $ri, $count);
                $no_checks = 1; # Already validated by enlist_new_test().
            }

            if ($token =~ /level\s+(\d+)/i) {

                my $level = $1;
                register_dependency($level, $value);
            }

            if ($token eq 'sticky actions') {

                # Will be used by each following Sticky URL.
                $sticky_actions = $value;
                if ($sticky_actions =~ /{[^}]*\s/) {
                    log_and_die("'Sticky Actions' with whitespace inside the " .
                                "action parameters are currently unsupported.");
                }
            }
            
            if ($si == -1 || $ri == -1) {
                # No beginning of a test detected yet,
                # so we don't care about any other test
                # attributes.
                next;
            }

            if ($token eq 'expect header') {

                l(LL_FILE_LOADING, "Detected expectation: " . $value);
                $regression_tests[$si][$ri]{'expect-header'} = $value;

            } elsif ($token eq 'tag') {
                
                next if ($ri == -1);

                my $tag = parse_tag($value);

                # We already checked in parse_tag() after filtering
                $no_checks = 1;

                l(LL_FILE_LOADING, "Detected TAG: " . $tag);

                # Save tag for all tests in this section
                do {
                    $regression_tests[$si][$ri]{'tag'} = $tag; 
                } while ($ri-- > 0);

                $si++;
                $ri = -1;

            } elsif ($token eq 'ignore' && $value =~ /Yes/i) {

                l(LL_FILE_LOADING, "Ignoring section: " . test_content_as_string($regression_tests[$si][$ri]));
                $regression_tests[$si][$ri]{'ignore'} = 1;
                $ignored++;

            } elsif ($token eq 'expect status code') {

                l(LL_FILE_LOADING, "Expecting status code: " . $value);
                $regression_tests[$si][$ri]{'expected-status-code'} = $value;

            } elsif ($token eq 'level') { # XXX: stupid name

                $value =~ s@(\d+).*@$1@;
                l(LL_FILE_LOADING, "Level: " . $value);
                $regression_tests[$si][$ri]{'level'} = $value;

            } elsif ($token eq 'method') {

                l(LL_FILE_LOADING, "Method: " . $value);
                $regression_tests[$si][$ri]{'method'} = $value;

            } elsif ($token eq 'redirect destination') {

                l(LL_FILE_LOADING, "Redirect destination: " . $value);
                $regression_tests[$si][$ri]{'redirect destination'} = $value;

            } elsif ($token eq 'url') {

                if (defined $sticky_actions) {
                    die "WTF? Attempted to overwrite Sticky Actions"
                        if defined ($regression_tests[$si][$ri]{'sticky-actions'});

                    l(LL_FILE_LOADING, "Sticky actions: " . $sticky_actions);
                    $regression_tests[$si][$ri]{'sticky-actions'} = $sticky_actions;
                } else {
                    log_and_die("Sticky URL without Sticky Actions: $value");
                }

            } else {

                # We don't use it, so we don't need
                $no_checks = 1;
                l(LL_STATUS, "Enabling no_checks for $token") unless $no_checks;
            }

            # XXX: Necessary?
            unless ($no_checks)  {
                check_for_forbidden_characters($value);
                check_for_forbidden_characters($token);
            }
        }
    }

    l(LL_FILE_LOADING, "Done loading " . $count . " regression tests." 
      . " Of which " . $ignored. " will be ignored)\n");
}

############################################################################
#
# Regression test executing functions
#
############################################################################

# Fisher Yates shuffle from Perl's "How do I shuffle an array randomly?" FAQ
sub fisher_yates_shuffle ($) {
    my $deck = shift;
    my $i = @$deck;
    while ($i--) {
        my $j = int rand($i+1);
        @$deck[$i,$j] = @$deck[$j,$i];
    }
}

sub execute_regression_tests () {

    our @regression_tests;
    my $loops = get_cli_option('loops');
    my $all_tests    = 0;
    my $all_failures = 0;
    my $all_successes = 0;

    unless (@regression_tests) {

        l(LL_STATUS, "No regression tests found.");
        return;
    }

    l(LL_STATUS, "Executing regression tests ...");

    while ($loops-- > 0) {

        my $successes = 0;
        my $tests = 0;
        my $failures;
        my $skipped = 0;

        if (cli_option_is_set('shuffle-tests')) {

            # Shuffle both the test sections and
            # the tests they contain.
            #
            # XXX: With the current data layout, shuffling tests
            #      from different sections isn't possible.
            #      Is this worth changing the layout?
            fisher_yates_shuffle(\@regression_tests);
            for (my $s = 0;  $s < @regression_tests; $s++) {
                fisher_yates_shuffle($regression_tests[$s]);
            }
        }

        for (my $s = 0;  $s < @regression_tests; $s++) {

            my $r = 0;

            while (defined $regression_tests[$s][$r]) {

                unless (cli_option_is_set('shuffle-tests')) {
                    die "Section id mismatch" if ($s != $regression_tests[$s][$r]{'section-id'});
                    die "Regression test id mismatch" if ($r != $regression_tests[$s][$r]{'regression-test-id'});
                }
                die "Internal error. Test executor missing."
                    unless defined $regression_tests[$s][$r]{executor};

                my $number = $regression_tests[$s][$r]{'number'};
                my $skip_reason = get_skip_reason($regression_tests[$s][$r]);

                if (defined $skip_reason) {

                    my $message = "Skipping test " . $number . ": " . $skip_reason . ".";
                    log_message($message) if (cli_option_is_set('show-skipped-tests'));
                    $skipped++;

                } else {

                    my $result = $regression_tests[$s][$r]{executor}($regression_tests[$s][$r]);

                    log_result($regression_tests[$s][$r], $result, $tests);

                    $successes += $result;
                    $tests++;
                    sleep(get_cli_option('sleep-time')) if (cli_option_is_set('sleep-time'));
                }
                $r++;
            }
        }
        $failures = $tests - $successes;

        log_message("Executed " . $tests . " regression tests. " .
            'Skipped ' . $skipped . '. ' . 
            $successes . " successes, " . $failures . " failures.");

        $all_tests     += $tests;
        $all_failures  += $failures;
        $all_successes += $successes;
    }

    if (get_cli_option('loops') > 1) {
        log_message("Total: Executed " . $all_tests . " regression tests. " .
            $all_successes . " successes, " . $all_failures . " failures.");
    }
}

sub get_skip_reason ($) {
    my $test = shift;
    my $skip_reason = undef;

    if ($test->{'ignore'}) {

        $skip_reason = "Ignore flag is set";

    } elsif (cli_option_is_set('test-number') and
             get_cli_option('test-number') != $test->{'number'}) {

        $skip_reason = "Only executing test " . get_cli_option('test-number');

    } else {

        $skip_reason = level_is_unacceptable($test->{'level'});
    }

    return $skip_reason;
}

sub level_is_unacceptable ($) {
    my $level = shift;
    my $min_level = get_cli_option('min-level');
    my $max_level = get_cli_option('max-level');
    my $required_level = cli_option_is_set('level') ?
        get_cli_option('level') : $level;
    my $reason = undef;

    if ($required_level != $level) {

        $reason = "Level doesn't match (" . $level .
                  " != " . $required_level . ")"

    } elsif ($level < $min_level) {

        $reason = "Level too low (" . $level . " < " . $min_level . ")";

    } elsif ($level > $max_level) {

        $reason = "Level too high (" . $level . " > " . $max_level . ")";

    } else {

        $reason = dependency_unsatisfied($level);
    }

    return $reason;
}

sub dependency_unsatisfied ($) {

    my $level = shift;
    our %dependencies;
    our @privoxy_config;
    our %privoxy_features;

    my $dependency_problem = undef;

    if (defined ($dependencies{$level}{'config line'})) {

        my $dependency = $dependencies{$level}{'config line'};
        $dependency_problem = "depends on config line matching: '" . $dependency . "'";

        foreach (@privoxy_config) {

            if (/$dependency/) {
                $dependency_problem = undef;
                last;
            }
        }

    }

    if (defined ($dependencies{$level}{'feature status'})
        and not defined $dependency_problem) {

        my $dependency = $dependencies{$level}{'feature status'};
        my ($feature, $status) = $dependency =~ /([^\s]*)\s+(Yes|No)/;

        unless (defined($privoxy_features{$feature})
                and ($privoxy_features{$feature} eq $status))
        {
            $dependency_problem = "depends on '" . $feature .
                "' being set to '" . $status . "'";
        }
    }

    return $dependency_problem;
}

sub register_dependency ($$) {

    my $level = shift;
    my $dependency = shift;
    our %dependencies;

    if ($dependency =~ /config line\s+(.*)/) {

        $dependencies{$level}{'config line'} = $1;

    } elsif ($dependency =~ /feature status\s+(.*)/) {

        $dependencies{$level}{'feature status'} = $1;

    } else {

        log_and_die("Didn't recognize dependency: $dependency.");
    }
}

sub execute_method_test ($) {

    my $test = shift;
    my $buffer_ref;
    my $status_code;
    my $method = $test->{'data'};

    my $curl_parameters = '';
    my $expected_status_code = $test->{'expected-status-code'};

    $curl_parameters .= '--request ' . $method . ' ';
    # Don't complain about the 'missing' body
    $curl_parameters .= '--head ' if ($method =~ /^HEAD$/i);

    $curl_parameters .= PRIVOXY_CGI_URL;

    $buffer_ref = get_page_with_curl($curl_parameters);
    $status_code = get_status_code($buffer_ref);

    return check_status_code_result($status_code, $expected_status_code);
}

sub execute_redirect_test ($) {

    my $test = shift;
    my $buffer_ref;
    my $status_code;

    my $curl_parameters = '';
    my $url = $test->{'data'};
    my $redirect_destination;
    my $expected_redirect_destination = $test->{'redirect destination'};

    # XXX: Check if a redirect actually applies before doing the request.
    #      otherwise the test may hit a real server in failure cases.

    $curl_parameters .= '--head ';

    $curl_parameters .= quote($url);

    $buffer_ref = get_page_with_curl($curl_parameters);
    $status_code = get_status_code($buffer_ref);

    if ($status_code ne "302") {
        l(LL_VERBOSE_FAILURE,
          "Ooops. Expected redirect to: '" . $expected_redirect_destination
          . "' but got a response with status code: " . $status_code);
        return 0;
    }
    foreach (@{$buffer_ref}) {
        if (/^Location: (.*)\r\n/) {
            $redirect_destination = $1;
            last;
        }
    }

    my $success = ($redirect_destination eq $expected_redirect_destination);

    unless ($success) {
        l(LL_VERBOSE_FAILURE,
          "Ooops. Expected redirect to: '" . $expected_redirect_destination
          . "' but the redirect leads to: '" . $redirect_destination. "'");
    }

    return $success;
}

sub execute_dumb_fetch_test ($) {

    my $test = shift;
    my $buffer_ref;
    my $status_code;

    my $curl_parameters = '';
    my $expected_status_code = $test->{'expected-status-code'};

    if (defined $test->{method}) {
        $curl_parameters .= '--request ' . quote($test->{method}) . ' ';
    }
    if ($test->{type} == TRUSTED_CGI_REQUEST) {
        $curl_parameters .= '--referer ' . quote(PRIVOXY_CGI_URL) . ' ';
    }

    $curl_parameters .= quote($test->{'data'});

    $buffer_ref = get_page_with_curl($curl_parameters);
    $status_code = get_status_code($buffer_ref);

    return check_status_code_result($status_code, $expected_status_code);
}

sub execute_block_test ($) {

    my $test = shift;
    my $url = $test->{'data'};
    my $final_results = get_final_results($url);

    return defined $final_results->{'+block'};
}

sub execute_sticky_actions_test ($) {

    my $test = shift;
    my $url = $test->{'data'};
    my $verified_actions = 0;
    # XXX: splitting currently doesn't work for actions whose parameters contain spaces.
    my @sticky_actions = split(/\s+/, $test->{'sticky-actions'});
    my $final_results = get_final_results($url);

    foreach my $sticky_action (@sticky_actions) {

        if (defined $final_results->{$sticky_action}) {
            # Exact match
            $verified_actions++;

        } elsif ($sticky_action =~ /-.*\{/) {

            # Disabled multi actions aren't explicitly listed as
            # disabled and thus have to be checked by verifying
            # that they aren't enabled.
            $verified_actions++;

        } else {
            l(LL_VERBOSE_FAILURE,
              "Ooops. '$sticky_action' is not among the final results.");
        }
    }

    return $verified_actions == @sticky_actions;
}

sub get_final_results ($) {

    my $url = shift;
    my $curl_parameters = '';
    my %final_results = ();
    my $final_results_reached = 0;

    die "Unacceptable characters in $url" if $url =~ m@[\\'"]@;
    # XXX: should be URL-encoded properly
    $url =~ s@%@%25@g;
    $url =~ s@\s@%20@g;
    $url =~ s@&@%26@g;
    $url =~ s@:@%3A@g;
    $url =~ s@/@%2F@g;

    $curl_parameters .= quote(PRIVOXY_CGI_URL . 'show-url-info?url=' . $url);

    foreach (@{get_cgi_page_or_else($curl_parameters)}) {

        $final_results_reached = 1 if (m@<h2>Final results:</h2>@);

        next unless ($final_results_reached);
        last if (m@</td>@);

        # Privoxy versions before 3.0.16 add a space
        # between action name and parameters, therefore
        # the " ?".
        if (m@<br>([-+])<a.*>([^>]*)</a>(?: ?(\{.*\}))?@) {
            my $action = $1.$2;
            my $parameter = $3;
            
            if (defined $parameter) {
                # In case the caller needs to check
                # the action and its parameter
                $final_results{$action . $parameter} = 1;
            }
            # In case the action doesn't have parameters
            # or the caller doesn't care for the parameter.
            $final_results{$action} = 1;
        }
    }

    return \%final_results;
}

sub check_status_code_result ($$) {

    my $status_code = shift;
    my $expected_status_code = shift;
    my $result = 0;

    unless (defined $status_code) {

        # XXX: should probably be caught earlier.
        l(LL_VERBOSE_FAILURE,
          "Ooops. We expected status code " . $expected_status_code . ", but didn't get any status code at all.");

    } elsif ($expected_status_code == $status_code) {

        $result = 1;
        l(LL_VERBOSE_SUCCESS,
          "Yay. We expected status code " . $expected_status_code . ", and received: " . $status_code . '.');

    } elsif (cli_option_is_set('fuzzer-feeding') and $status_code == 123) {

        l(LL_VERBOSE_FAILURE,
          "Oh well. Status code lost while fuzzing. Can't check if it was " . $expected_status_code . '.');

    } else {

        l(LL_VERBOSE_FAILURE,
          "Ooops. We expected status code " . $expected_status_code . ", but received: " . $status_code . '.');
    }
    
    return $result;
}

sub execute_client_header_regression_test ($) {

    my $test = shift;
    my $buffer_ref;
    my $header;

    $buffer_ref = get_show_request_with_curl($test);

    $header = get_header($buffer_ref, $test);

    return check_header_result($test, $header);
}

sub execute_server_header_regression_test ($) {

    my $test = shift;
    my $buffer_ref;
    my $header;

    $buffer_ref = get_head_with_curl($test);

    $header = get_server_header($buffer_ref, $test);

    return check_header_result($test, $header);
}

sub interpret_result ($) {
    my $success = shift;
    return $success ? "Success" : "Failure";
}

sub check_header_result ($$) {

    my $test = shift;
    my $header = shift;

    my $expect_header = $test->{'expect-header'};
    my $success = 0;

    if ($expect_header eq 'NO CHANGE') {

        $success = (defined($header) and $header eq $test->{'data'});

        unless ($success) {
            $header = "REMOVAL" unless defined $header;
            l(LL_VERBOSE_FAILURE,
              "Ooops. Got: '" . $header . "' while expecting: '" . $expect_header . "'");
        }

    } elsif ($expect_header eq 'REMOVAL') {

        # XXX: Use more reliable check here and make sure
        # the header has a different name.
        $success = not (defined($header) and $header eq $test->{'data'});

        unless ($success) {
            l(LL_VERBOSE_FAILURE,
              "Ooops. Expected removal but: '" . $header . "' is still there.");
        }

    } elsif ($expect_header eq 'SOME CHANGE') {

        $success = (defined($header) and $header ne $test->{'data'});

        unless  ($success) {
            $header = "REMOVAL" unless defined $header;
            l(LL_VERBOSE_FAILURE,
              "Ooops. Got: '" . $header . "' while expecting: SOME CHANGE");
        }

    } else {

        $success = (defined($header) and $header eq $expect_header);

        unless ($success) {
            $header = "No matching header" unless defined $header; # XXX: No header detected to be precise
            l(LL_VERBOSE_FAILURE,
              "Ooops. Got: '" . $header . "' while expecting: '" . $expect_header . "'");
        }
    }
    return $success;
}

sub get_header_name ($) {

    my $header = shift;

    $header =~ s@(.*?: ).*@$1@;

    return $header;
}

sub get_header ($$) {

    our $filtered_request = '';

    my $buffer_ref = shift;
    my $test = shift;

    my @buffer = @{$buffer_ref};

    my $expect_header = $test->{'expect-header'};

    die "get_header called with no expect header" unless defined $expect_header;

    my $line;
    my $processed_request_reached = 0;
    my $read_header = 0;
    my $processed_request = '';
    my $header;
    my $header_to_get;

    if ($expect_header eq 'REMOVAL'
     or $expect_header eq 'NO CHANGE'
     or $expect_header eq 'SOME CHANGE') {

        $expect_header = $test->{'data'};
    }

    $header_to_get = get_header_name($expect_header);

    foreach (@buffer) {

        # Skip everything before the Processed request
        if (/Processed Request/) {
            $processed_request_reached = 1;
            next;
        }
        next unless $processed_request_reached;

        # End loop after the Processed request
        last if (/<\/pre>/);

        # Ditch tags and leading/trailing white space.
        s@^\s*<.*?>@@g;
        s@\s*$@@g;

        # Decode characters we care about. 
        s@&quot;@"@g;

        $filtered_request .=  "\n" . $_;
         
        if (/^$header_to_get/) {
            $read_header = 1;
            $header = $_;
            last;
        }
    }

    return $header;
}

sub get_server_header ($$) {

    my $buffer_ref = shift;
    my $test = shift;

    my @buffer = @{$buffer_ref};

    my $expect_header = $test->{'expect-header'};
    my $header;
    my $header_to_get;

    # XXX: Should be caught before starting to test.
    log_and_die("No expect header for test " . $test->{'number'})
        unless defined $expect_header;

    if ($expect_header eq 'REMOVAL'
     or $expect_header eq 'NO CHANGE'
     or $expect_header eq 'SOME CHANGE') {

        $expect_header = $test->{'data'};
    }

    $header_to_get = get_header_name($expect_header);

    foreach (@buffer) {

        # XXX: should probably verify that the request
        # was actually answered by Fellatio.
        if (/^$header_to_get/) {
            $header = $_;
            $header =~ s@\s*$@@g;
            last;
        }
    }

    return $header;
}

sub get_status_code ($) {

    my $buffer_ref = shift;
    my @buffer = @{$buffer_ref}; 

    foreach (@buffer) {

        if (/^HTTP\/\d\.\d (\d{3})/) {

            return $1;

        } else {

            return '123' if cli_option_is_set('fuzzer-feeding');
            chomp;
            log_and_die('Unexpected buffer line: "' . $_ . '"');
        }
    }
}

sub get_test_keys () {
    return ('tag', 'data', 'expect-header', 'ignore');
}

# XXX: incomplete
sub test_content_as_string ($) {

    my $test = shift;

    my $s = "\n\t";

    foreach my $key (get_test_keys()) {
        $test->{$key} = 'Not set' unless (defined $test->{$key});
    }

    $s .= 'Tag: ' . $test->{'tag'};
    $s .= "\n\t";
    $s .= 'Set header: ' . $test->{'data'}; # XXX: adjust for other test types
    $s .= "\n\t";
    $s .= 'Expected header: ' . $test->{'expect-header'};
    $s .= "\n\t";
    $s .= 'Ignore: ' . $test->{'ignore'};

    return $s;
}

sub fuzz_header($) {
    my $header = shift;
    my $white_space = int(rand(2)) - 1 ? " " : "\t";

    $white_space = $white_space x (1 + int(rand(5)));

    # Only fuzz white space before the first quoted token.
    # (Privoxy doesn't touch white space inside quoted tokens
    # and modifying it would cause the tests to fail).
    $header =~ s@(^[^"]*?)\s@$1$white_space@g;

    return $header;
}

############################################################################
#
# HTTP fetch functions
#
############################################################################

sub get_cgi_page_or_else ($) {

    my $cgi_url = shift;
    my $content_ref = get_page_with_curl($cgi_url);
    my $status_code = get_status_code($content_ref);

    if (200 != $status_code) {

        my $log_message = "Failed to fetch Privoxy CGI Page. " .
                          "Received status code ". $status_code .
                          " while only 200 is acceptable.";

        if (cli_option_is_set('fuzzer-feeding')) {

            $log_message .= " Ignored due to fuzzer feeding.";
            l(LL_SOFT_ERROR, $log_message)

        } else {

            log_and_die($log_message);
        }
    }
    
    return $content_ref;
}

# XXX: misleading name
sub get_show_request_with_curl ($) {

    our $privoxy_cgi_url;
    my $test = shift;

    my $curl_parameters = ' ';
    my $header = $test->{'data'};

    if (cli_option_is_set('header-fuzzing')) {
        $header = fuzz_header($header);
    }

    # Enable the action to test
    $curl_parameters .= '-H \'X-Privoxy-Control: ' . $test->{'tag'} . '\' ';
    # The header to filter
    $curl_parameters .= '-H \'' . $header . '\' ';

    $curl_parameters .= ' ';
    $curl_parameters .= $privoxy_cgi_url;
    $curl_parameters .= 'show-request';

    return get_cgi_page_or_else($curl_parameters);
}

sub get_head_with_curl ($) {

    our $fellatio_url = FELLATIO_URL;
    my $test = shift;

    my $curl_parameters = ' ';

    # Enable the action to test
    $curl_parameters .= '-H \'X-Privoxy-Control: ' . $test->{'tag'} . '\' ';
    # The header to filter
    $curl_parameters .= '-H \'X-Gimme-Head-With: ' . $test->{'data'} . '\' ';
    $curl_parameters .= '--head ';

    $curl_parameters .= ' ';
    $curl_parameters .= $fellatio_url;

    return get_page_with_curl($curl_parameters);
}

sub get_page_with_curl ($) {

    our $proxy;

    my $parameters = shift;
    my @buffer;
    my $curl_line = CURL;
    my $retries_left = get_cli_option('retries') + 1;
    my $failure_reason;

    if (defined $proxy) {
        $curl_line .= ' --proxy ' . quote($proxy);
    }
    # We want to see the HTTP status code
    $curl_line .= " --include ";
    # Let Privoxy emit two log messages less.
    $curl_line .= ' -H \'Proxy-Connection:\' ' unless $parameters =~ /Proxy-Connection:/;
    $curl_line .= ' -H \'Connection: close\' ' unless $parameters =~ /Connection:/;
    # We don't care about fetch statistic.
    $curl_line .= " -s ";
    # We do care about the failure reason if any.
    $curl_line .= " -S ";
    # We want to advertise ourselves
    $curl_line .= " --user-agent '" . PRT_VERSION . "' ";
    # We aren't too patient
    $curl_line .= " --max-time '" . get_cli_option('max-time') . "' ";

    $curl_line .= $parameters;
    # XXX: still necessary?
    $curl_line .= ' 2>&1';

    l(LL_PAGE_FETCHING, "Executing: " . $curl_line);

    do {
        @buffer = `$curl_line`;

        if ($?) {
            log_and_die("Executing '$curl_line' failed.") unless @buffer;
            $failure_reason = array_as_string(\@buffer);
            chomp $failure_reason;
            l(LL_SOFT_ERROR, "Fetch failure: '" . $failure_reason . $! ."'");
        }
    } while ($? && --$retries_left);

    unless ($retries_left) {
        log_and_die("Running curl failed " . get_cli_option('retries') .
                    " times in a row. Last error: '" . $failure_reason . "'.");
    }

    return \@buffer;
}


############################################################################
#
# Log functions
#
############################################################################

sub array_as_string ($) {
    my $array_ref = shift;
    my $string = '';

    foreach (@{$array_ref}) {
        $string .= $_;
    }

    return $string;
}

sub show_test ($) {
    my $test = shift;
    log_message('Test is:' . test_content_as_string($test));
}

# Conditional log
sub l ($$) {
    our $log_level;
    my $this_level = shift;
    my $message = shift;

    log_message($message) if ($log_level & $this_level);
}

sub log_and_die ($) {
    my $message = shift;

    log_message('Oh noes. ' . $message . ' Fatal error. Exiting.');
    exit;
}

sub log_message ($) {

    my $message = shift;

    our $logfile;
    our $no_logging;
    our $leading_log_date;
    our $leading_log_time;

    my $time_stamp = '';
    my ( $sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst ) = localtime time;

    if ($leading_log_date || $leading_log_time) {

        if ($leading_log_date) {
            $year += 1900;
            $mon  += 1;
            $time_stamp = sprintf("%i-%.2i-%.2i", $year, $mon, $mday);
        }

        if ($leading_log_time) {
            $time_stamp .= ' ' if $leading_log_date;
            $time_stamp.= sprintf("%.2i:%.2i:%.2i", $hour, $min, $sec);
        }
        
        $message = $time_stamp . ": " . $message;
    }

    printf(STDERR "%s\n", $message);
}

sub log_result ($$) {

    our $verbose_test_description;
    our $filtered_request;

    my $test = shift;
    my $result = shift;
    my $number = shift;

    my $message = '';

    $message .= interpret_result($result);
    $message .= " for test ";
    $message .= $number;
    $message .= '/';
    $message .= $test->{'number'};
    $message .= '/';
    $message .= $test->{'section-id'};
    $message .= '/';
    $message .= $test->{'regression-test-id'};
    $message .= '.';

    if ($verbose_test_description) {

        if ($test->{'type'} == CLIENT_HEADER_TEST) {

            $message .= ' Header ';
            $message .= quote($test->{'data'});
            $message .= ' and tag ';
            $message .= quote($test->{'tag'});

        } elsif ($test->{'type'} == SERVER_HEADER_TEST) {

            $message .= ' Request Header ';
            $message .= quote($test->{'data'});
            $message .= ' and tag ';
            $message .= quote($test->{'tag'});

        } elsif ($test->{'type'} == DUMB_FETCH_TEST) {

            $message .= ' URL ';
            $message .= quote($test->{'data'});
            $message .= ' and expected status code ';
            $message .= quote($test->{'expected-status-code'});

        } elsif ($test->{'type'} == TRUSTED_CGI_REQUEST) {

            $message .= ' CGI URL ';
            $message .= quote($test->{'data'});
            $message .= ' and expected status code ';
            $message .= quote($test->{'expected-status-code'});

        } elsif ($test->{'type'} == METHOD_TEST) {

            $message .= ' HTTP method ';
            $message .= quote($test->{'data'});
            $message .= ' and expected status code ';
            $message .= quote($test->{'expected-status-code'});

        } elsif ($test->{'type'} == BLOCK_TEST) {

            $message .= ' Supposedly-blocked URL: ';
            $message .= quote($test->{'data'});

        } elsif ($test->{'type'} == STICKY_ACTIONS_TEST) {

            $message .= ' Sticky Actions: ';
            $message .= quote($test->{'sticky-actions'});
            $message .= ' and URL: ';
            $message .= quote($test->{'data'});

        } elsif ($test->{'type'} == REDIRECT_TEST) {

            $message .= ' Redirected URL: ';
            $message .= quote($test->{'data'});
            $message .= ' and redirect destination: ';
            $message .= quote($test->{'redirect destination'});

        } else {

            die "Incomplete support for test type " . $test->{'type'} .  " detected.";
        }
    }

    log_message($message) if (!$result or cli_option_is_set('verbose'));
}

sub quote ($) {
    my $s = shift;
    return '\'' . $s . '\'';
}

sub print_version () {
    printf PRT_VERSION . "\n" . 'Copyright (C) 2007-2011 Fabian Keil <fk@fabiankeil.de>' . "\n";
}

sub list_test_types () {
    my %test_types = (
        'Client header test'  => CLIENT_HEADER_TEST,
        'Server header test'  =>  2,
        'Dumb fetch test'     =>  3,
        'Method test'         =>  4,
        'Sticky action test'  =>  5,
        'Trusted CGI test'    =>  6,
        'Block test'          =>  7,
        'Redirect test'       => 108,
    );

    print "\nThe supported test types and their default levels are:\n";
    foreach my $test_type (sort { $test_types{$a} <=> $test_types{$b} } keys %test_types) {
        printf "     %-20s -> %3.d\n", $test_type, $test_types{$test_type};
    }
}

sub help () {

    our %cli_options;

    print_version();

    print << "    EOF"

Options and their default values if they have any:
    [--debug $cli_options{'debug'}]
    [--forks $cli_options{'forks'}]
    [--fuzzer-address]
    [--fuzzer-feeding]
    [--help]
    [--header-fuzzing]
    [--level]
    [--local-test-file]
    [--loops $cli_options{'loops'}]
    [--max-level $cli_options{'max-level'}]
    [--max-time $cli_options{'max-time'}]
    [--min-level $cli_options{'min-level'}]
    [--privoxy-address]
    [--retries $cli_options{'retries'}]
    [--show-skipped-tests]
    [--shuffle-tests]
    [--sleep-time $cli_options{'sleep-time'}]
    [--test-number]
    [--verbose]
    [--version]
    EOF
    ;

    list_test_types();

    print << "    EOF"

Try "perldoc $0" for more information
    EOF
    ;

    exit(0);
}

sub init_cli_options () {

    our %cli_options;
    our $log_level;

    $cli_options{'debug'}     = $log_level;
    $cli_options{'forks'}     = CLI_FORKS;
    $cli_options{'loops'}     = CLI_LOOPS;
    $cli_options{'max-level'} = CLI_MAX_LEVEL;
    $cli_options{'max-time'}  = CLI_MAX_TIME;
    $cli_options{'min-level'} = CLI_MIN_LEVEL;
    $cli_options{'sleep-time'}= CLI_SLEEP_TIME;
    $cli_options{'retries'}   = CLI_RETRIES;
}

sub parse_cli_options () {

    our %cli_options;
    our $log_level;

    init_cli_options();

    GetOptions (
        'debug=i'            => \$cli_options{'debug'},
        'forks=i'            => \$cli_options{'forks'},
        'fuzzer-address=s'   => \$cli_options{'fuzzer-address'},
        'fuzzer-feeding'     => \$cli_options{'fuzzer-feeding'},
        'header-fuzzing'     => \$cli_options{'header-fuzzing'},
        'help'               => \&help,
        'level=i'            => \$cli_options{'level'},
        'local-test-file=s'  => \$cli_options{'local-test-file'},
        'loops=i'            => \$cli_options{'loops'},
        'max-level=i'        => \$cli_options{'max-level'},
        'max-time=i'         => \$cli_options{'max-time'},
        'min-level=i'        => \$cli_options{'min-level'},
        'privoxy-address=s'  => \$cli_options{'privoxy-address'},
        'retries=i'          => \$cli_options{'retries'},
        'shuffle-tests'      => \$cli_options{'shuffle-tests'},
        'show-skipped-tests' => \$cli_options{'show-skipped-tests'},
        'sleep-time=i'       => \$cli_options{'sleep-time'},
        'test-number=i'      => \$cli_options{'test-number'},
        'verbose'            => \$cli_options{'verbose'},
        'version'            => sub {print_version && exit(0)}
    ) or exit(1);
    $log_level |= $cli_options{'debug'};
}

sub cli_option_is_set ($) {

    our %cli_options;
    my $cli_option = shift;

    return defined $cli_options{$cli_option};
}

sub get_cli_option ($) {

    our %cli_options;
    my $cli_option = shift;

    die "Unknown CLI option: $cli_option" unless defined $cli_options{$cli_option};

    return $cli_options{$cli_option};
}

sub init_proxy_settings($) {

    my $choice = shift;
    our $proxy = undef;

    if (($choice eq 'fuzz-proxy') and cli_option_is_set('fuzzer-address')) {
        $proxy = get_cli_option('fuzzer-address');
    }

    if ((not defined $proxy) or ($choice eq 'vanilla-proxy')) {

        if (cli_option_is_set('privoxy-address')) {
            $proxy .=  get_cli_option('privoxy-address');
        }
    }
}

sub start_forks($) {
    my $forks = shift;

    log_and_die("Invalid --fork value: " . $forks . ".") if ($forks < 0);

    foreach my $fork (1 .. $forks) {
        log_message("Starting fork $fork");
        my $pid = fork();
        if (defined $pid && !$pid) {
            return;
        }
    }
}

sub main () {

    init_our_variables();
    parse_cli_options();
    init_proxy_settings('vanilla-proxy');
    load_regression_tests();
    init_proxy_settings('fuzz-proxy');
    start_forks(get_cli_option('forks')) if cli_option_is_set('forks');
    execute_regression_tests();
}

main();

=head1 NAME

B<privoxy-regression-test> - A regression test "framework" for Privoxy.

=head1 SYNOPSIS

B<privoxy-regression-test> [B<--debug bitmask>] [B<--forks> forks]
[B<--fuzzer-feeding>] [B<--fuzzer-feeding>] [B<--help>] [B<--level level>]
[B<--local-test-file testfile>] [B<--loops count>] [B<--max-level max-level>]
[B<--max-time max-time>] [B<--min-level min-level>] B<--privoxy-address proxy-address>
[B<--retries retries>] [B<--test-number test-number>]
[B<--show-skipped-tests>] [B<--sleep-time> seconds] [B<--verbose>]
[B<--version>]

=head1 DESCRIPTION

Privoxy-Regression-Test is supposed to one day become
a regression test suite for Privoxy. It's not quite there
yet, however, and can currently only test header actions,
check the returned status code for requests to arbitrary
URLs and verify which actions are applied to them.

Client header actions are tested by requesting
B<http://p.p/show-request> and checking whether
or not Privoxy modified the original request as expected.

The original request contains both the header the action-to-be-tested
acts upon and an additional tagger-triggering header that enables
the action to test.

Applied actions are checked through B<http://p.p/show-url-info>.

=head1 CONFIGURATION FILE SYNTAX

Privoxy-Regression-Test's configuration is embedded in
Privoxy action files and loaded through Privoxy's web interface.

It makes testing a Privoxy version running on a remote system easier
and should prevent you from updating your tests without updating Privoxy's
configuration accordingly.

A client-header-action test section looks like this:

    # Set Header    = Referer: http://www.example.org.zwiebelsuppe.exit/
    # Expect Header = Referer: http://www.example.org/
    {+client-header-filter{hide-tor-exit-notation} -hide-referer}
    TAG:^client-header-filter\{hide-tor-exit-notation\}$

The example above causes Privoxy-Regression-Test to set
the header B<Referer: http://www.example.org.zwiebelsuppe.exit/>
and to expect it to be modified to
B<Referer: http://www.example.org/>.

When testing this section, Privoxy-Regression-Test will set the header
B<X-Privoxy-Control: client-header-filter{hide-tor-exit-notation}>
causing the B<privoxy-control> tagger to create the tag
B<client-header-filter{hide-tor-exit-notation}> which will finally
cause Privoxy to enable the action section.

Note that the actions itself are only used by Privoxy,
Privoxy-Regression-Test ignores them and will be happy
as long as the expectations are satisfied.

A fetch test looks like this:

    # Fetch Test = http://p.p/user-manual
    # Expect Status Code = 302

It tells Privoxy-Regression-Test to request B<http://p.p/user-manual>
and to expect a response with the HTTP status code B<302>. Obviously that's
not a very thorough test and mainly useful to get some code coverage
for Valgrind or to verify that the templates are installed correctly.

If you want to test CGI pages that require a trusted
referer, you can use:

    # Trusted CGI Request = http://p.p/edit-actions

It works like ordinary fetch tests, but sets the referer
header to a trusted value.

If no explicit status code expectation is set, B<200> is used.

To verify that a URL is blocked, use:

    # Blocked URL = http://www.example.com/blocked

To verify that a specific set of actions is applied to an URL, use:

    # Sticky Actions = +block{foo} +handle-as-empty-document -handle-as-image
    # URL = http://www.example.org/my-first-url

The sticky actions will be checked for all URLs below it
until the next sticky actions directive.

To verify that requests for a URL get redirected, use:

    # Redirected URL = http://www.example.com/redirect-me
    # Redirect Destination = http://www.example.org/redirected

=head1 TEST LEVELS

All tests have test levels to let the user
control which ones to execute (see I<OPTIONS> below). 
Test levels are either set with the B<Level> directive,
or implicitly through the test type.

Redirect tests default to level 108, block tests to level 7,
fetch tests to level 6, "Sticky Actions" tests default to
level 5, tests for trusted CGI requests to level 3 and
client-header-action tests to level 1.

The current redirect test level is above the default
max-level value as failed tests will result in outgoing
connections. Use the B<--max-level> option to run them
as well.

=head1 OPTIONS

B<--debug bitmask> Add the bitmask provided as integer
to the debug settings.

B<--forks forks> Number of forks to start before executing
the regression tests. This is mainly useful for stress-testing.

B<--fuzzer-address> Listening address used when executing
the regression tests. Useful to make sure that the requests
to load the regression tests don't fail due to fuzzing.

B<--fuzzer-feeding> Ignore some errors that would otherwise
cause Privoxy-Regression-Test to abort the test because
they shouldn't happen in normal operation. This option is
intended to be used if Privoxy-Regression-Test is only
used to feed a fuzzer in which case there's a high chance
that Privoxy gets an invalid request and returns an error
message.

B<--help> Shows available command line options.

B<--header-fuzzing> Modifies linear white space in
headers in a way that should not affect the test result.

B<--level level> Only execute tests with the specified B<level>. 

B<--local-test-file test-file> Do not get the tests
through Privoxy's web interface, but use a single local
file. Not recommended for testing Privoxy, but can be useful
to "misappropriate" Privoxy-Regression-Test to test other
stuff, like webserver configurations.

B<--loop count> Loop through the regression tests B<count> times. 
Useful to feed a fuzzer, or when doing stress tests with
several Privoxy-Regression-Test instances running at the same
time.

B<--max-level max-level> Only execute tests with a B<level>
below or equal to the numerical B<max-level>.

B<--max-time max-time> Give Privoxy B<max-time> seconds
to return data. Increasing the default may make sense when
Privoxy is run through Valgrind, decreasing the default may
make sense when Privoxy-Regression-Test is used to feed
a fuzzer.

B<--min-level min-level> Only execute tests with a B<level>
above or equal to the numerical B<min-level>.

B<--privoxy-address proxy-address> Privoxy's listening address.
If it's not set, the value of the environment variable http_proxy
will be used. B<proxy-address> has to be specified in http_proxy
syntax.

B<--retries retries> Retry B<retries> times.

B<--test-number test-number> Only run the test with the specified
number.

B<--show-skipped-tests> Log skipped tests even if verbose mode is off.

B<--shuffle-tests> Shuffle test sections and their tests before
executing them. When combined with B<--forks>, this can increase
the chances of detecting race conditions. Of course some problems
are easier to detect without this option.

B<--sleep-time seconds> Wait B<seconds> between tests. Useful when
debugging issues with systems that don't log with millisecond precision.

B<--verbose> Log successful tests as well. By default only
the failures are logged.

B<--version> Print version and exit.

The second dash is optional, options can be shortened,
as long as there are no ambiguities.

=head1 PRIVOXY CONFIGURATION

Privoxy-Regression-Test is shipped with B<regression-tests.action>
which aims to test all official client-header modifying actions
and can be used to verify that the templates and the user manual
files are installed correctly.

To use it, it has to be copied in Privoxy's configuration
directory, and afterwards referenced in Privoxy's configuration
file with the line:

    actionsfile regression-tests.action

In general, its tests are supposed to work without changing
any other action files, unless you already added lots of
taggers yourself. If you are using taggers that cause problems,
you might have to temporary disable them for Privoxy's CGI pages.

Some of the regression tests rely on Privoxy features that
may be disabled in your configuration. Tests with a level below
7 are supposed to work with all Privoxy configurations (provided
you didn't build with FEATURE_GRACEFUL_TERMINATION).

Tests with level 9 require Privoxy to deliver the User Manual,
tests with level 12 require the CGI editor to be enabled.

=head1 CAVEATS

Expect the configuration file syntax to change with future releases.

=head1 LIMITATIONS

As Privoxy's B<show-request> page only shows client headers,
Privoxy-Regression-Test can't use it to test Privoxy actions
that modify server headers.

As Privoxy-Regression-Test relies on Privoxy's tag feature to
control the actions to test, it currently only works with
Privoxy 3.0.7 or later.

At the moment Privoxy-Regression-Test fetches Privoxy's
configuration page through I<curl>(1), therefore you have to
have I<curl> installed, otherwise you won't be able to run
Privoxy-Regression-Test in a meaningful way.

=head1 SEE ALSO

privoxy(1) curl(1)

=head1 AUTHOR

Fabian Keil <fk@fabiankeil.de>

=cut
