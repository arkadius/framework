/*
 * Copyright 2006-2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb
package util

import java.util.{Calendar, Date}

import org.joda.time.DateTimeZone
import org.specs2.execute.AsResult
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.specs2.time.NoTimeConversions
import org.scalacheck.Gen._
import org.scalacheck.Prop._

import common._
import TimeHelpers._



/**
 * Systems under specification for TimeHelpers.
 */
object TimeHelpersSpec extends Specification with ScalaCheck with TimeAmountsGen with NoTimeConversions {
  "TimeHelpers Specification".title

  "A TimeSpan" can {
    "be created from a number of milliseconds" in {
      forAllTimeZones(TimeSpan(3000) must_== TimeSpan(3 * 1000))
    }
    "be created from a number of seconds" in {
      forAllTimeZones(3.seconds must_== TimeSpan(3 * 1000))
    }
    "be created from a number of minutes" in {
      forAllTimeZones(3.minutes must_== TimeSpan(3 * 60 * 1000))
    }
    "be created from a number of hours" in {
      forAllTimeZones(3.hours must_== TimeSpan(3 * 60 * 60 * 1000))
    }
    "be created from a number of days" in {
      forAllTimeZones(3.days must_== TimeSpan(3 * 24 * 60 * 60 * 1000))
    }
    "be created from a number of weeks" in {
      forAllTimeZones(3.weeks must_== TimeSpan(3 * 7 * 24 * 60 * 60 * 1000))
    }
    "be created from a number of months" in {
      forAllTimeZones(3.months must_== 3.months)
    }
    "be created from a number of years" in {
      forAllTimeZones(3.years must_== 3.years)
    }
    "be converted implicitly to a date starting from the epoch time" in {
      forAllTimeZones(3.seconds.after(new Date(0)) must beTrue)
    }
    "be converted to a date starting from the epoch time, using the date method" in {
      forAllTimeZones(3.seconds.after(new Date(0)) must beTrue)
    }
    "be implicitly converted to a Long" in {
      forAllTimeZones((3.seconds == 3000L) must_== true)
    }
    "be compared to an int" in {
      forAllTimeZones {
        (3.seconds == 3000) must_== true
        (3.seconds != 2000) must_== true
      }
    }
    "be compared to a long" in {
      forAllTimeZones {
        (3.seconds == 3000L) must_== true
        (3.seconds != 2000L) must_== true
      }
    }
    "be compared to another TimeSpan" in {
      forAllTimeZones {
        3.seconds must_== 3.seconds
        3.seconds must_!= 2.seconds
      }
    }
    "be compared to another object" in {
      forAllTimeZones(3.seconds must_!= "string")
    }
  }

  def forAllTimeZones[T: AsResult](f: => T) = {
    import collection.convert.wrapAsScala._
    for {
      timeZoneId <- DateTimeZone.getAvailableIDs.toSeq
    } yield {
      DateTimeZone.setDefault(DateTimeZone.forID(timeZoneId))
      f
    }
  }

  "A TimeSpan" should {
    "return a new TimeSpan representing the sum of the 2 times when added with another TimeSpan" in {
      3.seconds + 3.seconds must_== 6.seconds
    }
    "return a new TimeSpan representing the difference of the 2 times when substracted with another TimeSpan" in {
      3.seconds - 4.seconds must_== (-1).seconds
    }
    "have a later method returning a date relative to now plus the time span" in {
      val expectedTime = new Date().getTime + 3.seconds.millis

      3.seconds.later.getTime must beCloseTo(expectedTime, 1000L)
    }
    "have an ago method returning a date relative to now minus the time span" in {
      val expectedTime = new Date().getTime - 3.seconds.millis

      3.seconds.ago.getTime must beCloseTo(expectedTime, 1000L)
    }
    "have a toString method returning the relevant number of weeks, days, hours, minutes, seconds, millis" in {
      val conversionIsOk = forAll(timeAmounts)((t: TimeAmounts) => { val (timeSpanToString, timeSpanAmounts) = t
        timeSpanAmounts forall { case (amount, unit) =>
          amount >= 1  &&
          timeSpanToString.contains(amount.toString) || true }
      })
      val timeSpanStringIsPluralized = forAll(timeAmounts)((t: TimeAmounts) => { val (timeSpanToString, timeSpanAmounts) = t
        timeSpanAmounts forall { case (amount, unit) =>
               amount > 1  && timeSpanToString.contains(unit + "s") ||
               amount == 1 && timeSpanToString.contains(unit) ||
               amount == 0 && !timeSpanToString.contains(unit)
        }
      })
      check(conversionIsOk && timeSpanStringIsPluralized)
    }
  }

  "the TimeHelpers" should {
    "provide a 'seconds' function transforming a number of seconds into millis" in {
      seconds(3) must_== 3 * 1000
    }
    "provide a 'minutes' function transforming a number of minutes into millis" in {
      minutes(3) must_== 3 * 60 * 1000
    }
    "provide a 'hours' function transforming a number of hours into milliss" in {
      hours(3) must_== 3 * 60 * 60 * 1000
    }
    "provide a 'days' function transforming a number of days into millis" in {
      days(3) must_== 3 * 24 * 60 * 60 * 1000
    }
    "provide a 'weeks' function transforming a number of weeks into millis" in {
      weeks(3) must_== 3 * 7 * 24 * 60 * 60 * 1000
    }
    "provide a noTime function on Date objects to transform a date into a date at the same day but at 00:00" in {
      hourFormat(now.noTime) must_== "00:00:00"
    }

    "make sure noTime does not change the day" in {
      dateFormatter.format(0.days.ago.noTime) must_== dateFormatter.format(new Date())
      dateFormatter.format(3.days.ago.noTime) must_== dateFormatter.format(new Date(millis - (3 * 24 * 60 * 60 * 1000)))
    }

    "provide a day function returning the day of month corresponding to a given date (relative to UTC)" in {
      day(today.setTimezone(utc).setDay(3).getTime) must_== 3
    }
    "provide a month function returning the month corresponding to a given date" in {
      month(today.setTimezone(utc).setMonth(4).getTime) must_== 4
    }
    "provide a year function returning the year corresponding to a given date" in {
      year(today.setTimezone(utc).setYear(2008).getTime) must_== 2008
    }
    "provide a millisToDays function returning the number of days since the epoch time" in {
      millisToDays(new Date(0).getTime) must_== 0
      millisToDays(today.setYear(1970).setMonth(0).setDay(1).getTime.getTime) must_== 0 // the epoch time
      // on the 3rd day after the epoch time, 2 days are passed
      millisToDays(today.setTimezone(utc).setYear(1970).setMonth(0).setDay(3).getTime.getTime) must_== 2
    }
    "provide a daysSinceEpoch function returning the number of days since the epoch time" in {
      daysSinceEpoch must_== millisToDays(now.getTime)
    }
    "provide a time function creating a new Date object from a number of millis" in {
      time(1000) must_== new Date(1000)
    }
    "provide a calcTime function returning the time taken to evaluate a block in millis and the block's result" in {
      val (time, result) = calcTime((1 to 10).reduceLeft[Int](_ + _))
      time.toInt must beCloseTo(0, 1000)  // it should take less than 1 second!
      result must_== 55
    }

    "provide a hourFormat function to format the time of a date object" in {
      hourFormat(Calendar.getInstance(utc).noTime.getTime) must_== "00:00:00"
    }

    "provide a formattedDateNow function to format todays date" in {
      formattedDateNow must beMatching("\\d\\d\\d\\d/\\d\\d/\\d\\d")
    }
    "provide a formattedTimeNow function to format now's time with the TimeZone" in {
      val regex = "\\d\\d:\\d\\d (....?|GMT((\\+|\\-)\\d\\d:00)?)"
      "10:00 CEST" must beMatching(regex)
      "10:00 GMT+02:00" must beMatching(regex)
      "10:00 GMT" must beMatching(regex)
      "10:00 XXX" must beMatching(regex)
      formattedTimeNow must beMatching(regex)
    }

    "provide a parseInternetDate function to parse a string formatted using the internet format" in {
      parseInternetDate(internetDateFormatter.format(now)).getTime.toLong must beCloseTo(now.getTime.toLong, 1000L)
    }
    "provide a parseInternetDate function returning new Date(0) if the input date cant be parsed" in {
      parseInternetDate("unparsable") must_== new Date(0)
    }
    "provide a toInternetDate function formatting a date to the internet format" in {
      toInternetDate(now) must beMatching("..., \\d* ... \\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d .*")
    }
    "provide a toDate returning a Full(date) from many kinds of objects" in {
      val d = now
      List(null, Nil, None, Failure("", Empty, Empty)) forall { toDate(_) must_== Empty }
      List(Full(d), Some(d), List(d)) forall { toDate(_) must_== Full(d) }

      toDate(internetDateFormatter.format(d)) must beLike {
        case Full(converted) =>
          converted.getTime.toLong must beCloseTo(d.getTime.toLong, 1000L)
      }
    }
  }

  "The Calendar class" should {
    "have a setDay method setting the day of month and returning the updated Calendar" in {
      day(today.setTimezone(utc).setDay(1).getTime) must_== 1
    }
    "have a setMonth method setting the month and returning the updated Calendar" in {
      month(today.setTimezone(utc).setMonth(0).getTime) must_== 0
    }
    "have a setYear method setting the year and returning the updated Calendar" in {
      year(today.setTimezone(utc).setYear(2008).getTime) must_== 2008
    }
    "have a setTimezone method to setting the time zone and returning the updated Calendar" in {
      today.setTimezone(utc).getTimeZone must_== utc
    }
    "have a noTime method to setting the time to 00:00:00 and returning the updated Calendar" in {
      hourFormat(today.noTime.getTime) must_== "00:00:00"
    }
  }
}


trait TimeAmountsGen {

  type TimeAmounts = (String, List[(Int, String)])

  val timeAmounts =
    for {
      w <- choose(0, 2)
      d <- choose(0, 6)
      h <- choose(0, 23)
      m <- choose(0, 59)
      s <- choose(0, 59)
      ml <- choose(0, 999)
    }
    yield (
      TimeSpan(weeks(w) + days(d) + hours(h) + minutes(m) + seconds(s) + ml).toString,
      (w, "week") :: (d, "day") :: (h, "hour") :: (m, "minute") :: (s, "second") :: (ml, "milli") :: Nil
    )
}

