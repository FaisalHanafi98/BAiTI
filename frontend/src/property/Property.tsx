import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import type { components } from '../api/types.gen'
import { describeError, isWriteUnconfirmed } from '../api/errors'
import { browserSession, useSession } from '../auth/browser'
import { BlockEditor, UnitEditor, WriteError, useEscapeDismiss } from './Editors'

type Schema = components['schemas']
type Block = Schema['BlockResponse']
type Unit = Schema['UnitResponse']
type UnitView = Schema['UnitView']
type Occupancy = NonNullable<Unit['status']>
type Entry<T> = { mode: 'create' } | { mode: 'edit' | 'delete'; record: T }

const api = browserSession.api
const pageSize = 20
function fullUnit(unit: UnitView): unit is Unit & { id: string } {
  return 'id' in unit && typeof unit.id === 'string'
}

function Feedback({ error, retry }: { error: unknown; retry?: () => void }) {
  const { t } = useTranslation()
  const detail = describeError(error, 'property')
  return <div className="row callout" data-tone="danger"><p className="error" role="alert">{t(detail.key, detail)}</p>
    {retry && <button className="secondary" onClick={retry}>{t('refreshView')}</button>}
  </div>
}

function Pagination({ page, total, change }: { page: number; total: number; change: (page: number) => void }) {
  const { t } = useTranslation()
  if (total < 2 && page === 0) return null
  return <div className="row">
    <button className="secondary" disabled={!page} onClick={() => change(page - 1)}>{t('previous')}</button>
    <span>{t('page', { current: page + 1, total: Math.max(1, total) })}</span>
    <button className="secondary" disabled={page + 1 >= total} onClick={() => change(page + 1)}>{t('next')}</button>
  </div>
}

function DeletePanel({ name, pending, error, reload, close, confirm }: {
  name?: string; pending: boolean; error: unknown; reload: () => Promise<unknown>; close: () => void; confirm: () => void;
}) {
  const { t } = useTranslation()
  useEscapeDismiss(pending, close)
  return <section className="editor" aria-label={t('deleteConfirm', { name })}>
    <h3>{t('deleteConfirm', { name })}</h3>
    <WriteError error={error} reload={reload} />
    <div className="row">
      <button className="secondary" disabled={pending} onClick={close}>{t('cancel')}</button>
      <button className="danger" disabled={pending || isWriteUnconfirmed(error)} onClick={confirm}>{t(pending ? 'saving' : 'remove')}</button>
    </div>
  </section>
}

function Blocks() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const [editor, setEditor] = useState<Entry<Block>>()
  const triggerRef = useRef<HTMLButtonElement | null>(null)
  const queries = useQueryClient()
  const blocks = useQuery({
    queryKey: ['property', 'blocks', page],
    queryFn: async ({ signal }) => (await api.get<Schema['PropertyPageBlockResponse']>('/api/blocks', {
      params: { page, size: pageSize }, signal,
    })).data,
  })
  const action = useMutation({
    mutationFn: async (data?: Schema['CreateBlockRequest']) => {
      if (editor?.mode === 'delete' && editor.record.id) await api.delete('/api/blocks/' + editor.record.id)
      else if (editor?.mode === 'edit' && editor.record.id) await api.put('/api/blocks/' + editor.record.id, data)
      else if (editor?.mode === 'create') await api.post('/api/blocks', data)
    },
    onSuccess: async () => {
      setEditor(undefined)
      if (editor?.mode === 'delete' && blocks.data?.content?.length === 1) setPage(Math.max(0, page - 1))
      await queries.invalidateQueries({ queryKey: ['property'] })
    },
  })
  async function reload() {
    await blocks.refetch({ throwOnError: true })
    action.reset()
  }
  function edit(next?: Entry<Block>, trigger?: HTMLButtonElement) {
    if (trigger) triggerRef.current = trigger
    action.reset()
    setEditor(next)
    if (!next && triggerRef.current?.isConnected) triggerRef.current.focus()
  }
  return <section className="card">
    <div className="row between"><h2>{t('blocks')}</h2><button disabled={action.isPending} onClick={(event) => edit({ mode: 'create' }, event.currentTarget)}>{t('newBlock')}</button></div>
    {editor?.mode === 'delete' && <DeletePanel name={editor.record.name} pending={action.isPending} error={action.error} reload={reload}
      close={() => edit()} confirm={() => action.mutate(undefined)} />}
    {editor && editor.mode !== 'delete' && <BlockEditor key={editor.mode === 'edit' ? editor.record.id : 'new'}
      record={editor.mode === 'edit' ? editor.record : undefined}
      pending={action.isPending} error={action.error} reload={reload} close={() => edit()} submit={(body) => action.mutate(body)} />}
    {blocks.isPending && <p role="status">{t('loadingProperty')}</p>}
    {blocks.isError && <Feedback error={blocks.error} retry={() => void blocks.refetch()} />}
    {blocks.data && (blocks.data.content?.length ? <div className="table-wrap">
      <table aria-label={t('blocks')}><thead><tr><th scope="col">{t('name')}</th><th scope="col">{t('description')}</th><th scope="col">{t('actions')}</th></tr></thead>
        <tbody>{blocks.data.content.map((block) => <tr key={block.id}>
          <td>{block.name}</td><td>{block.description}</td><td><div className="row">
            <button className="secondary" disabled={action.isPending} aria-label={t('editNamed', { name: block.name })}
              onClick={(event) => edit({ mode: 'edit', record: block }, event.currentTarget)}>{t('edit')}</button>
            <button className="secondary" disabled={action.isPending} aria-label={t('deleteNamed', { name: block.name })}
              onClick={(event) => edit({ mode: 'delete', record: block }, event.currentTarget)}>{t('remove')}</button>
          </div></td>
        </tr>)}</tbody></table>
    </div> : <p>{t('emptyBlocks')}</p>)}
    <Pagination page={page} total={blocks.data?.totalPages ?? 0} change={setPage} />
  </section>
}

async function blockOptions(signal: AbortSignal) {
  const blocks: Block[] = []
  let page = 0
  let total = 1
  while (page < total) {
    const { data } = await api.get<Schema['PropertyPageBlockResponse']>('/api/blocks', {
      params: { page, size: 100 }, signal,
    })
    blocks.push(...(data.content ?? []))
    total = data.totalPages ?? 0
    page++
  }
  return blocks
}

function Units({ guard, canManage, canReadBlocks }: { guard: boolean; canManage: boolean; canReadBlocks: boolean }) {
  const { t } = useTranslation()
  const queries = useQueryClient()
  const [page, setPage] = useState(0)
  const [blockId, setBlockId] = useState('')
  const [occupancy, setOccupancy] = useState<Occupancy | ''>('')
  const [editor, setEditor] = useState<Entry<Unit & { id: string }>>()
  const triggerRef = useRef<HTMLButtonElement | null>(null)
  const blocks = useQuery({
    queryKey: ['property', 'block-options'], queryFn: ({ signal }) => blockOptions(signal), enabled: canReadBlocks && !guard,
  })
  const units = useQuery({
    queryKey: ['property', 'units', page, blockId, occupancy, guard],
    queryFn: async ({ signal }) => (await api.get<Schema['PropertyPageUnitView']>('/api/units', {
      params: { page, size: pageSize, ...(!guard && {
        blockId: canReadBlocks && blockId ? blockId : undefined,
        status: occupancy || undefined,
      }) }, signal,
    })).data,
  })
  const action = useMutation({
    mutationFn: async (data?: Schema['UpdateUnitRequest']) => {
      if (editor?.mode === 'delete') await api.delete('/api/units/' + editor.record.id)
      else if (editor?.mode === 'edit') await api.put('/api/units/' + editor.record.id, data)
      else if (editor?.mode === 'create') await api.post('/api/units', data)
    },
    onSuccess: async () => {
      setEditor(undefined)
      if (editor?.mode === 'delete' && units.data?.content?.length === 1) setPage(Math.max(0, page - 1))
      await queries.invalidateQueries({ queryKey: ['property', 'units'] })
    },
  })
  async function reload() {
    await units.refetch({ throwOnError: true })
    action.reset()
  }
  function edit(next?: Entry<Unit & { id: string }>, trigger?: HTMLButtonElement) {
    if (trigger) triggerRef.current = trigger
    action.reset()
    setEditor(next)
    if (!next && triggerRef.current?.isConnected) triggerRef.current.focus()
  }
  return <section className="card">
    <div className="row between"><h2>{t(guard ? 'guardUnits' : 'units')}</h2>
      {canManage && <button disabled={!blocks.data?.length || action.isPending} onClick={(event) => edit({ mode: 'create' }, event.currentTarget)}>{t('newUnit')}</button>}
    </div>
    {canManage && blocks.isSuccess && !blocks.data.length && <p>{t('unitNeedsBlock')}</p>}
    {canReadBlocks && blocks.isError && <Feedback error={blocks.error} retry={() => void blocks.refetch()} />}
    {canManage && editor?.mode === 'delete' && <DeletePanel name={editor.record.unitNo} pending={action.isPending}
      error={action.error} reload={reload} close={() => edit()} confirm={() => action.mutate(undefined)} />}
    {canManage && editor && editor.mode !== 'delete' && <UnitEditor key={editor.mode === 'edit' ? editor.record.id : 'new'}
      record={editor.mode === 'edit' ? editor.record : undefined} blocks={blocks.data ?? []}
      pending={action.isPending} error={action.error} reload={reload} close={() => edit()} submit={(body) => action.mutate(body)} />}
    {!guard && <div className="row filters">
      {canReadBlocks && <label>{t('block')}
        <select value={blockId} onChange={(event) => { setBlockId(event.target.value); setPage(0) }}>
          <option value="">{t('allBlocks')}</option>
          {blocks.data?.map((block) => <option key={block.id} value={block.id}>{block.name}</option>)}
        </select>
      </label>}
      <label>{t('occupancy')}
        <select value={occupancy} onChange={(event) => { setOccupancy(event.target.value as Occupancy | ''); setPage(0) }}>
          <option value="">{t('allStates')}</option><option value="VACANT">{t('vacant')}</option>
          <option value="OCCUPIED">{t('occupied')}</option>
        </select>
      </label>
    </div>}
    {units.isPending && <p role="status">{t('loadingProperty')}</p>}
    {units.isError && <Feedback error={units.error} retry={() => void units.refetch()} />}
    {units.data && (units.data.content?.length ? <div className="table-wrap">
      <table aria-label={t(guard ? 'guardUnits' : 'units')}><thead><tr><th scope="col">{t('unitNo')}</th>
        {!guard && <><th scope="col">{t('block')}</th><th scope="col">{t('floorNo')}</th><th scope="col">{t('type')}</th>
          <th scope="col">{t('occupancy')}</th>{canManage && <th scope="col">{t('actions')}</th>}</>}
      </tr></thead><tbody>{units.data.content.map((unit, index) => {
        const detail = fullUnit(unit) ? unit : undefined
        return <tr key={detail?.id ?? index}><td>{unit.unitNo}</td>
          {!guard && <><td>{detail?.blockName}</td><td>{detail?.floorNo}</td><td>{detail?.type}</td>
            <td>{detail?.status && <span className="status" data-status={detail.status}>{t(detail.status === 'VACANT' ? 'vacant' : 'occupied')}</span>}</td>
            {canManage && <td>{detail && <div className="row">
              <button className="secondary" disabled={action.isPending} aria-label={t('editNamed', { name: detail.unitNo })}
                onClick={(event) => edit({ mode: 'edit', record: detail }, event.currentTarget)}>{t('edit')}</button>
              <button className="secondary" disabled={action.isPending} aria-label={t('deleteNamed', { name: detail.unitNo })}
                onClick={(event) => edit({ mode: 'delete', record: detail }, event.currentTarget)}>{t('remove')}</button>
            </div>}</td>}</>}
        </tr>
      })}</tbody></table>
    </div> : <p>{t('emptyUnits')}</p>)}
    <Pagination page={page} total={units.data?.totalPages ?? 0} change={setPage} />
  </section>
}

export function Property() {
  const { t } = useTranslation()
  const session = useSession()
  const guard = session.kind === 'signed-in' && session.user.role === 'Guard'
  const canReadBlocks = browserSession.can('BLOCK_MANAGE')
  return <>
    <h1>{t('property')}</h1>
    {canReadBlocks && <Blocks />}
    {browserSession.can('UNIT_READ_ALL') && <Units guard={guard}
      canReadBlocks={canReadBlocks} canManage={!guard && canReadBlocks && browserSession.can('UNIT_MANAGE')} />}
  </>
}
