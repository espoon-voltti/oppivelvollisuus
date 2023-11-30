import { parse } from 'date-fns'
import React, { useEffect, useMemo, useState } from 'react'

import { formatDate, parseDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import {
  GroupOfInputRows,
  LabeledInputL,
  LabeledInputM,
  LabeledInputS,
  RowOfInputs
} from '../shared/layout'
import { Label } from '../shared/typography'

import { StudentDetails, StudentInput } from './api'

interface CreateProps {
  onChange: (validInput: StudentInput | null) => void
}
interface ViewProps {
  student: StudentDetails
  editing: false
}
interface EditProps {
  student: StudentDetails
  editing: true
  onChange: (validInput: StudentInput | null) => void
}
type Props = CreateProps | ViewProps | EditProps

function isCreating(p: Props): p is CreateProps {
  return !('student' in p)
}

function isViewing(p: Props): p is ViewProps {
  return 'student' in p && !p.editing
}

export const StudentForm = React.memo(function StudentForm(props: Props) {
  const [valpasLink, setValpasLink] = useState(
    isCreating(props) ? '' : props.student.valpasLink
  )
  const [ssn, setSsn] = useState(isCreating(props) ? '' : props.student.ssn)
  const [firstName, setFirstName] = useState(
    isCreating(props) ? '' : props.student.firstName
  )
  const [lastName, setLastName] = useState(
    isCreating(props) ? '' : props.student.lastName
  )
  const [dateOfBirth, setDateOfBirth] = useState(
    isCreating(props) || !props.student.dateOfBirth
      ? ''
      : formatDate(props.student.dateOfBirth)
  )
  const [phone, setPhone] = useState(
    isCreating(props) ? '' : props.student.phone
  )
  const [email, setEmail] = useState(
    isCreating(props) ? '' : props.student.email
  )
  const [address, setAdress] = useState(
    isCreating(props) ? '' : props.student.address
  )

  useEffect(() => {
    if (ssn.length >= 6) {
      try {
        const dateFromSsn = parse(ssn, 'ddMMyy', new Date())
        setDateOfBirth(formatDate(dateFromSsn))
      } catch (e) {
        // ignore
      }
    }
  }, [ssn])

  const isValid =
    firstName.trim() !== '' &&
    lastName.trim() !== '' &&
    (dateOfBirth === '' || parseDate(dateOfBirth) !== undefined)

  const validInput: StudentInput | null = useMemo(
    () =>
      isValid
        ? {
            valpasLink: valpasLink.trim(),
            ssn: ssn.trim(),
            firstName: firstName.trim(),
            lastName: lastName.trim(),
            dateOfBirth: dateOfBirth.trim()
              ? parseDate(dateOfBirth.trim()) ?? null
              : null,
            phone: phone.trim(),
            email: email.trim(),
            address: address.trim()
          }
        : null,
    [
      isValid,
      valpasLink,
      ssn,
      firstName,
      lastName,
      dateOfBirth,
      phone,
      email,
      address
    ]
  )

  useEffect(() => {
    if (!isViewing(props)) {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <GroupOfInputRows $gapSize="L">
      <RowOfInputs $gapSize="m">
        <LabeledInputS>
          <Label>Hetu</Label>
          {isViewing(props) ? (
            <span>{props.student.ssn || '-'}</span>
          ) : (
            <InputField onChange={setSsn} value={ssn} />
          )}
        </LabeledInputS>
        <LabeledInputS>
          <Label>Syntymäaika</Label>
          {isViewing(props) ? (
            <span>
              {props.student.dateOfBirth
                ? formatDate(props.student.dateOfBirth)
                : '-'}
            </span>
          ) : (
            <InputField onChange={setDateOfBirth} value={dateOfBirth} />
          )}
        </LabeledInputS>
        <LabeledInputL>
          <Label>Valpas linkki</Label>
          {isViewing(props) ? (
            props.student.valpasLink ? (
              <a
                href={props.student.valpasLink}
                target="_blank"
                rel="noreferrer"
              >
                {props.student.valpasLink}
              </a>
            ) : (
              <span>-</span>
            )
          ) : (
            <InputField onChange={setValpasLink} value={valpasLink} />
          )}
        </LabeledInputL>
      </RowOfInputs>
      <RowOfInputs $gapSize="L">
        <LabeledInputM>
          <Label>Etunimi</Label>
          {isViewing(props) ? (
            <span>{props.student.firstName}</span>
          ) : (
            <InputField onChange={setFirstName} value={firstName} />
          )}
        </LabeledInputM>
        <LabeledInputM>
          <Label>Sukunimi</Label>
          {isViewing(props) ? (
            <span>{props.student.lastName}</span>
          ) : (
            <InputField onChange={setLastName} value={lastName} />
          )}
        </LabeledInputM>
      </RowOfInputs>
      <RowOfInputs $gapSize="L">
        <LabeledInputM>
          <Label>Puhelinnumero</Label>
          {isViewing(props) ? (
            <span>{props.student.phone || '-'}</span>
          ) : (
            <InputField onChange={setPhone} value={phone} />
          )}
        </LabeledInputM>
        <LabeledInputM>
          <Label>Sähköposti</Label>
          {isViewing(props) ? (
            <span>{props.student.email || '-'}</span>
          ) : (
            <InputField onChange={setEmail} value={email} />
          )}
        </LabeledInputM>
        <LabeledInputL>
          <Label>Lähiosoite</Label>
          {isViewing(props) ? (
            <span>{props.student.address || '-'}</span>
          ) : (
            <InputField onChange={setAdress} value={address} />
          )}
        </LabeledInputL>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})
