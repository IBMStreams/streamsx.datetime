#ifndef DATETIME_H_
#define DATETIME_H_

#include <SPL/Runtime/Function/TimeFunctions.h>
#include <streams_boost/date_time/c_local_time_adjustor.hpp>
#include <streams_boost/date_time/gregorian/gregorian_types.hpp>
#include <streams_boost/date_time/posix_time/posix_time.hpp>
#include <streams_boost/fusion/include/adapt_struct.hpp>
#include <streams_boost/spirit/include/phoenix.hpp>
#include <streams_boost/spirit/include/qi.hpp>
#include <math.h>

namespace com { namespace ibm { namespace streamsx { namespace datetime { namespace convert {

	using namespace streams_boost::posix_time;
	using namespace streams_boost::spirit::qi;
	using streams_boost::gregorian::date;
	using streams_boost::phoenix::bind;
	using streams_boost::phoenix::ref;
	using streams_boost::phoenix::throw_;

	typedef std::string::const_iterator iter_t;
	typedef streams_boost::iterator_range<iter_t> iterRange_t;
	typedef streams_boost::date_time::c_local_adjustor<ptime> cLocalAdjustor;

	struct DateTime {
		unsigned short year;
		unsigned short month;
		unsigned short day;
		unsigned short hour;
		unsigned short min;
		unsigned short sec;
		double nanos;
		bool isLocal;
		short tz_hour;
		short tz_min;
	};

	// Spirit Qi grammar to parse a date
	struct DateTimeGrammar : grammar<iter_t, DateTime()> {

	    DateTimeGrammar() : DateTimeGrammar::base_type(dateTime, "DateTimeGrammar") {
	    	int_parser<short, 10, 2, 2> strict_short2;
	    	int_parser<unsigned short, 10, 2, 2> strict_ushort2;
	    	int_parser<unsigned short, 10, 4, 4> strict_ushort4;
	    	real_parser<double, strict_ureal_policies<double> > strict_double;

	    	delim = omit[punct | space];

	    	// Parse the input data directly to DateTime structure
	   		dateTime %= skip(space)[eps] >												// '>' means any parsing error after it will throw an exception
	   			strict_ushort4 >> delim >> strict_ushort2 >> delim >> strict_ushort2 >>	// date
				(lit('T') | skip(space)[&eoi][throw_("No time")] | delim) >>			// 'T', delimiter or end of input (no time found)
	   			ushort_ >> delim >> ushort_ >> delim >> ushort_ >>						// time
				-strict_double >> ((&char_("Z+-") >> attr(false)) | attr(true)) >>		// nanos and TZ
				-strict_short2 >> -delim >> -strict_short2;								// TZ offset
	    }

	private:
	    rule<iter_t> delim;
	    rule<iter_t, DateTime()> dateTime;
	};


	// Get an offset for a default timezone including DST
	inline time_duration getOffset(ptime const& timeDate) {
		return cLocalAdjustor::utc_to_local(timeDate) - timeDate;
	}

	// parseDateTime accepts date as a string and an optional flag if to throw exception when time is missing (default: false)
	inline SPL::timestamp parseDateTime(std::string const& ts, bool missingTimeExc = false) {

		// Define DateTimeGrammar as a static variable - thread safe
		static DateTimeGrammar dateTimeGrammar;
		// Define an epoch base (1970/1/1)
		static const ptime epoch(from_time_t(0));

		DateTime dateTime = {};

		iter_t first = ts.begin();
		iter_t last = ts.end();

		try {
			parse(first, last, dateTimeGrammar, dateTime);
		}
		catch (expectation_failure<iter_t> const& ex) {
		    throw std::runtime_error("Invalid format of date '" + ts + "'");
		}
		catch (...) {
			dateTime.isLocal = true;
			if(missingTimeExc)
				throw std::runtime_error("Invalid format of date, time not found: '" + ts + "'");
		}

		// If timezone offset hours is negative then negate minutes too
		if(dateTime.tz_hour < 0) dateTime.tz_min = -dateTime.tz_min;

		// Create PointTime from date and time_duration
		const date parsedDate(dateTime.year, dateTime.month, dateTime.day);
		const time_duration parsedTime(dateTime.hour, dateTime.min, dateTime.sec);
		const ptime timeDate = ptime(parsedDate, parsedTime);

		// Calculate seconds from epoch
		const time_duration::sec_type totalSecs = dateTime.isLocal
			? (timeDate - epoch - getOffset(timeDate)).total_seconds() // no timezone provided - use the local offset to get UTC
			: (timeDate - epoch - hours(dateTime.tz_hour) - minutes(dateTime.tz_min)).total_seconds(); // the timezone offset exists - get UTC

		return SPL::Functions::Time::createTimestamp(totalSecs, dateTime.nanos * 1000000000);
	}
}}}}}

// Adapt DateTime structure to the Fusion format - enables DateTimeGrammar to parse directly to it
STREAMS_BOOST_FUSION_ADAPT_STRUCT(
	com::ibm::streamsx::datetime::convert::DateTime,
	(unsigned short, year)
	(unsigned short, month)
	(unsigned short, day)
	(unsigned short, hour)
	(unsigned short, min)
	(unsigned short, sec)
	(double, nanos)
	(bool, isLocal)
	(short, tz_hour)
	(short, tz_min)
)

#endif /* DATETIME_H_ */
