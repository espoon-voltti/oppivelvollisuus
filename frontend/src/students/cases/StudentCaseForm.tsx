import React, { useEffect, useMemo, useState } from 'react'

import { EmployeeUser } from '../../employees/api'
import { formatDate, parseDate } from '../../shared/dates'
import { InputField } from '../../shared/form/InputField'
import { Select } from '../../shared/form/Select'
import {
  FixedWidthDiv,
  GroupOfInputRows,
  LabeledInput,
  RowOfInputs
} from '../../shared/layout'
import { Label } from '../../shared/typography'

import { CaseSourceFields, StudentCase, StudentCaseInput } from './api'
import {
  CaseSource,
  caseSourceNames,
  caseSources,
  OtherNotifier,
  otherNotifierNames,
  otherNotifiers,
  ValpasNotifier,
  valpasNotifierNames,
  valpasNotifiers
} from './enums'

interface SharedProps {
  employees: EmployeeUser[]
}
interface CreateProps extends SharedProps {
  mode: 'CREATE'
  onChange: (validInput: StudentCaseInput | null) => void
}
interface ViewProps extends SharedProps {
  mode: 'VIEW'
  studentCase: StudentCase
}
interface EditProps extends SharedProps {
  mode: 'EDIT'
  studentCase: StudentCase
  onChange: (validInput: StudentCaseInput | null) => void
}
type Props = CreateProps | ViewProps | EditProps

export const StudentCaseForm = React.memo(function StudentCaseForm(
  props: Props
) {
  const [openedAt, setOpenedAt] = useState(
    formatDate(
      props.mode === 'CREATE' ? new Date() : props.studentCase.openedAt
    )
  )
  const [assignedTo, setAssignedTo] = useState(
    props.mode === 'CREATE' || !props.studentCase.assignedTo
      ? null
      : props.employees.find(
          (e) => e.id === props.studentCase.assignedTo?.id
        ) ?? null
  )
  const [source, setSource] = useState<CaseSource | null>(
    props.mode === 'CREATE' ? null : props.studentCase.source
  )
  const [sourceValpas, setSourceValpas] = useState<ValpasNotifier | null>(
    props.mode === 'CREATE' ? null : props.studentCase.sourceValpas
  )
  const [sourceOther, setSourceOther] = useState<OtherNotifier | null>(
    props.mode === 'CREATE' ? null : props.studentCase.sourceOther
  )
  const [sourceContact, setSourceContact] = useState<string>(
    props.mode === 'CREATE' ? '' : props.studentCase.sourceContact
  )

  const validInput: StudentCaseInput | null = useMemo(() => {
    const openedAtDate = parseDate(openedAt.trim())

    if (!openedAtDate) return null
    if (!source) return null

    const validSourceFields: CaseSourceFields | null =
      source === 'VALPAS_NOTICE' && sourceValpas
        ? {
            source,
            sourceValpas,
            sourceOther: null
          }
        : source === 'VALPAS_AUTOMATIC_CHECK'
          ? {
              source,
              sourceValpas: null,
              sourceOther: null
            }
          : source === 'OTHER' && sourceOther
            ? {
                source,
                sourceValpas: null,
                sourceOther
              }
            : null

    if (!validSourceFields) return null

    return {
      openedAt: openedAtDate,
      assignedTo: assignedTo?.id ?? null,
      ...validSourceFields,
      sourceContact
    }
  }, [openedAt, assignedTo, source, sourceValpas, sourceOther, sourceContact])

  useEffect(() => {
    if (props.mode !== 'VIEW') {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <GroupOfInputRows>
      <RowOfInputs>
        <FixedWidthDiv $cols={6}>
          <LabeledInput $cols={3}>
            <Label>Ilmoitettu</Label>
            {props.mode === 'VIEW' ? (
              <span>{formatDate(props.studentCase.openedAt)}</span>
            ) : (
              <InputField onChange={setOpenedAt} value={openedAt} />
            )}
          </LabeledInput>
        </FixedWidthDiv>
        <LabeledInput $cols={4}>
          <Label>Ohjaaja</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.studentCase.assignedTo?.name ?? '-'}</span>
          ) : (
            <Select<EmployeeUser>
              items={props.employees}
              selectedItem={assignedTo}
              getItemValue={(e) => e.id}
              getItemLabel={(e) => `${e.firstName} ${e.lastName}`}
              placeholder="Ei ohjaajaa"
              onChange={setAssignedTo}
            />
          )}
        </LabeledInput>
      </RowOfInputs>
      <RowOfInputs>
        <LabeledInput $cols={3}>
          <Label>Mitä kautta tieto saapunut?</Label>
          {props.mode === 'VIEW' ? (
            <span>{caseSourceNames[props.studentCase.source]}</span>
          ) : (
            <Select<CaseSource>
              items={caseSources}
              selectedItem={source}
              placeholder="Valitse"
              getItemLabel={(item) => caseSourceNames[item]}
              onChange={(item) => setSource(item)}
            />
          )}
        </LabeledInput>
        <LabeledInput $cols={3}>
          {props.mode === 'VIEW' ? (
            <>
              {(props.studentCase.source === 'VALPAS_NOTICE' ||
                props.studentCase.source === 'OTHER') && (
                <Label>Ilmoittanut taho</Label>
              )}
              {props.studentCase.source === 'VALPAS_NOTICE' && (
                <span>
                  {valpasNotifierNames[props.studentCase.sourceValpas]}
                </span>
              )}
              {props.studentCase.source === 'OTHER' && (
                <span>{otherNotifierNames[props.studentCase.sourceOther]}</span>
              )}
            </>
          ) : (
            <>
              {(source === 'VALPAS_NOTICE' || source === 'OTHER') && (
                <Label>Ilmoittanut taho</Label>
              )}
              {source === 'VALPAS_NOTICE' && (
                <Select<ValpasNotifier>
                  items={valpasNotifiers}
                  selectedItem={sourceValpas}
                  placeholder="Valitse"
                  getItemLabel={(item) => valpasNotifierNames[item]}
                  onChange={(item) => setSourceValpas(item)}
                />
              )}
              {source === 'OTHER' && (
                <Select<OtherNotifier>
                  items={otherNotifiers}
                  selectedItem={sourceOther}
                  placeholder="Valitse"
                  getItemLabel={(item) => otherNotifierNames[item]}
                  onChange={(item) => setSourceOther(item)}
                />
              )}
            </>
          )}
        </LabeledInput>
        <LabeledInput>
          <Label>Ilmoitettajan yhteystiedot</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.studentCase.sourceContact || '-'}</span>
          ) : (
            <InputField onChange={setSourceContact} value={sourceContact} />
          )}
        </LabeledInput>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})
