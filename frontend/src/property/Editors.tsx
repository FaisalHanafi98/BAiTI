import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { describeError, isWriteUnconfirmed } from '../api/errors'
import type { components } from '../api/types.gen'

type Schema = components['schemas']
type Editor<T> = {
  pending: boolean
  error: unknown
  reload: () => Promise<unknown>
  close: () => void
  submit: (data: T) => void
}
const required = z.string().refine((value) => value.trim().length > 0, 'errors.required')
const blockSchema = z.object({ name: required, description: z.string() })
const unitSchema = z.object({
  unitNo: required, blockId: required, type: z.string(), floorNo: z.string(),
  status: z.enum(['VACANT', 'OCCUPIED']),
})

export function useEscapeDismiss(pending: boolean, close: () => void) {
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !pending) {
        event.preventDefault()
        close()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [pending, close])
}

function EditorActions({ pending, blocked, close }: { pending: boolean; blocked: boolean; close: () => void }) {
  const { t } = useTranslation()
  return <div className="row">
    <button className="secondary" type="button" disabled={pending} onClick={close}>{t('cancel')}</button>
    <button disabled={pending || blocked}>{t(pending ? 'saving' : 'save')}</button>
  </div>
}

export function WriteError({ error, reload }: { error: unknown; reload: () => Promise<unknown> }) {
  const { t } = useTranslation()
  const [reloading, setReloading] = useState(false)
  const [reloadFailed, setReloadFailed] = useState(false)
  if (!error) return null
  if (!isWriteUnconfirmed(error)) {
    const detail = describeError(error, 'property')
    return <p className="callout error" data-tone="danger" role="alert">{t(detail.key, detail)}</p>
  }
  return <div className="callout" data-tone="warning">
    <p className="error" role="alert">{t('errors.writeUnconfirmed')}</p>
    {reloadFailed && <p className="error" role="alert">{t('errors.reloadFailed')}</p>}
    <button type="button" className="secondary" disabled={reloading} onClick={async () => {
      setReloading(true)
      setReloadFailed(false)
      try { await reload() } catch { setReloadFailed(true) }
      finally { setReloading(false) }
    }}>{t(reloading ? 'loadingProperty' : 'refreshView')}</button>
  </div>
}

export function BlockEditor({ record, pending, error, reload, close, submit }: Editor<Schema['CreateBlockRequest']> & { record?: Schema['BlockResponse'] }) {
  const { t } = useTranslation()
  useEscapeDismiss(pending, close)
  const form = useForm({
    resolver: zodResolver(blockSchema), defaultValues: { name: record?.name ?? '', description: record?.description ?? '' },
  })
  const invalid = form.formState.errors.name?.message
  return <form className="editor" noValidate onSubmit={form.handleSubmit((values) => { if (!pending && !isWriteUnconfirmed(error)) submit(values) })}>
    <h3>{t(record ? 'editBlock' : 'newBlock')}</h3><WriteError error={error} reload={reload} />
    <label><span>{t('name')}</span><input id="block-name" aria-invalid={!!invalid} aria-describedby={invalid ? 'block-name-error' : undefined} {...form.register('name')} />
      </label>{invalid && <span className="error" id="block-name-error">{t(invalid)}</span>}
    <label>{t('description')}<textarea {...form.register('description')} /></label>
    <EditorActions pending={pending} blocked={isWriteUnconfirmed(error)} close={close} />
  </form>
}

export function UnitEditor({ record, blocks, pending, error, reload, close, submit }:
  Editor<Schema['UpdateUnitRequest']> & { record?: Schema['UnitResponse']; blocks: Schema['BlockResponse'][] }) {
  const { t } = useTranslation()
  useEscapeDismiss(pending, close)
  const form = useForm({
    resolver: zodResolver(unitSchema),
    defaultValues: {
      unitNo: record?.unitNo ?? '', blockId: record?.blockId ?? '', status: record?.status ?? 'VACANT',
      type: record?.type ?? '', floorNo: record?.floorNo ?? '',
    },
  })
  return <form className="editor" noValidate onSubmit={form.handleSubmit((values) => { if (!pending && !isWriteUnconfirmed(error)) submit(values) })}>
    <h3>{t(record ? 'editUnit' : 'newUnit')}</h3><WriteError error={error} reload={reload} />
    <label>{t('block')}<select aria-invalid={!!form.formState.errors.blockId} aria-describedby={form.formState.errors.blockId ? 'unit-block-error' : undefined} {...form.register('blockId')}>
      <option value="">{t('selectBlock')}</option>
      {blocks.map((block) => <option key={block.id} value={block.id}>{block.name}</option>)}
    </select></label>{form.formState.errors.blockId && <span className="error" id="unit-block-error">{t('errors.required')}</span>}
    <label>{t('unitNo')}<input aria-invalid={!!form.formState.errors.unitNo} aria-describedby={form.formState.errors.unitNo ? 'unit-number-error' : undefined} {...form.register('unitNo')} />
      </label>{form.formState.errors.unitNo && <span className="error" id="unit-number-error">{t('errors.required')}</span>}
    <label>{t('type')}<input {...form.register('type')} /></label>
    <label>{t('floorNo')}<input {...form.register('floorNo')} /></label>
    <label>{t('occupancy')}<select {...form.register('status')}><option value="VACANT">{t('vacant')}</option>
      <option value="OCCUPIED">{t('occupied')}</option></select></label>
    <EditorActions pending={pending} blocked={isWriteUnconfirmed(error)} close={close} />
  </form>
}
