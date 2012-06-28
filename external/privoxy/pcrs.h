#ifndef PCRS_H_INCLUDED
#define PCRS_H_INCLUDED

/*********************************************************************
 *
 * File        :  $Source: /cvsroot/ijbswa/current/pcrs.h,v $
 *
 * Purpose     :  Header file for pcrs.c
 *
 * Copyright   :  see pcrs.c
 *
 *********************************************************************/

#define PCRS_H_VERSION "$Id: pcrs.h,v 1.17 2009/05/16 13:27:20 fabiankeil Exp $"


#ifndef _PCRE_H
#include <pcre.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Constants:
 */

#define FALSE 0
#define TRUE 1

/* Capacity */
#define PCRS_MAX_SUBMATCHES  33     /* Maximum number of capturing subpatterns allowed. MUST be <= 99! FIXME: Should be dynamic */
#define PCRS_MAX_MATCH_INIT  40     /* Initial amount of matches that can be stored in global searches */
#define PCRS_MAX_MATCH_GROW  1.6    /* Factor by which storage for matches is extended if exhausted */

/*
 * PCRS error codes
 *
 * They are supposed to be handled together with PCRE error
 * codes and have to start with an offset to prevent overlaps.
 *
 * PCRE 6.7 uses error codes from -1 to -21, PCRS error codes
 * below -100 should be safe for a while.
 */
#define PCRS_ERR_NOMEM           -100      /* Failed to acquire memory. */
#define PCRS_ERR_CMDSYNTAX       -101      /* Syntax of s///-command */
#define PCRS_ERR_STUDY           -102      /* pcre error while studying the pattern */
#define PCRS_ERR_BADJOB          -103      /* NULL job pointer, pattern or substitute */
#define PCRS_WARN_BADREF         -104      /* Backreference out of range */
#define PCRS_WARN_TRUNCATION     -105      /* At least one pcrs variable was too big,
                                            * only the first part was used. */

/* Flags */
#define PCRS_GLOBAL          1      /* Job should be applied globally, as with perl's g option */
#define PCRS_TRIVIAL         2      /* Backreferences in the substitute are ignored */
#define PCRS_SUCCESS         4      /* Job did previously match */


/*
 * Data types:
 */

/* A compiled substitute */

typedef struct {
  char  *text;                                   /* The plaintext part of the substitute, with all backreferences stripped */
  size_t length;                                 /* The substitute may not be a valid C string so we can't rely on strlen(). */
  int    backrefs;                               /* The number of backreferences */
  int    block_offset[PCRS_MAX_SUBMATCHES];      /* Array with the offsets of all plaintext blocks in text */
  size_t block_length[PCRS_MAX_SUBMATCHES];      /* Array with the lengths of all plaintext blocks in text */
  int    backref[PCRS_MAX_SUBMATCHES];           /* Array with the backref number for all plaintext block borders */
  int    backref_count[PCRS_MAX_SUBMATCHES + 2]; /* Array with the number of references to each backref index */
} pcrs_substitute;


/*
 * A match, including all captured subpatterns (submatches)
 * Note: The zeroth is the whole match, the PCRS_MAX_SUBMATCHES + 0th
 * is the range before the match, the PCRS_MAX_SUBMATCHES + 1th is the
 * range after the match.
 */

typedef struct {
  int    submatches;                               /* Number of captured subpatterns */
  int    submatch_offset[PCRS_MAX_SUBMATCHES + 2]; /* Offset for each submatch in the subject */
  size_t submatch_length[PCRS_MAX_SUBMATCHES + 2]; /* Length of each submatch in the subject */
} pcrs_match;


/* A PCRS job */

typedef struct PCRS_JOB {
  pcre *pattern;                            /* The compiled pcre pattern */
  pcre_extra *hints;                        /* The pcre hints for the pattern */
  int options;                              /* The pcre options (numeric) */
  int flags;                                /* The pcrs and user flags (see "Flags" above) */
  pcrs_substitute *substitute;              /* The compiled pcrs substitute */
  struct PCRS_JOB *next;                    /* Pointer for chaining jobs to joblists */
} pcrs_job;


/*
 * Prototypes:
 */

/* Main usage */
extern pcrs_job        *pcrs_compile_command(const char *command, int *errptr);
extern pcrs_job        *pcrs_compile(const char *pattern, const char *substitute, const char *options, int *errptr);
extern int              pcrs_execute(pcrs_job *job, const char *subject, size_t subject_length, char **result, size_t *result_length);
extern int              pcrs_execute_list(pcrs_job *joblist, char *subject, size_t subject_length, char **result, size_t *result_length);

/* Freeing jobs */
extern pcrs_job        *pcrs_free_job(pcrs_job *job);
extern void             pcrs_free_joblist(pcrs_job *joblist);

/* Info on errors: */
extern const char *pcrs_strerror(const int error);

extern int pcrs_job_is_dynamic(char *job);
extern char pcrs_get_delimiter(const char *string);
extern char *pcrs_execute_single_command(const char *subject, const char *pcrs_command, int *hits);
/*
 * Variable/value pair for dynamic pcrs commands.
 */
struct pcrs_variable
{
   const char *name;
   char *value;
   int static_value;
};

extern pcrs_job *pcrs_compile_dynamic_command(char *pcrs_command, const struct pcrs_variable v[], int *error);

/* Only relevant for maximum pcrs variable size */
#ifndef PCRS_BUFFER_SIZE
#define PCRS_BUFFER_SIZE 4000
#endif /* ndef PCRS_BUFFER_SIZE */

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* ndef PCRS_H_INCLUDED */

/*
  Local Variables:
  tab-width: 3
  end:
*/
